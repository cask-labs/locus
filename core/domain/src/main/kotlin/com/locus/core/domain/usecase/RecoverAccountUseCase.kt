package com.locus.core.domain.usecase

import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_ACCESS_KEY
import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_SECRET_KEY
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.S3Client
import com.locus.core.domain.infrastructure.StackProvisioningService
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.util.ArnUtils
import com.locus.core.domain.util.AuthUtils
import java.util.UUID
import javax.inject.Inject

class RecoverAccountUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val configRepository: ConfigurationRepository,
        private val s3Client: S3Client,
        private val resourceProvider: ResourceProvider,
        private val stackProvisioningService: StackProvisioningService,
    ) {
        suspend operator fun invoke(
            creds: BootstrapCredentials,
            bucketName: String,
        ): LocusResult<Unit> {
            val history = mutableListOf<String>()

            suspend fun updateStep(step: String) {
                authRepository.updateProvisioningState(ProvisioningState.Working(step, history.toList()))
            }

            suspend fun completeStep(step: String) {
                if (history.size >= ProvisioningState.MAX_HISTORY_SIZE) {
                    history.removeAt(0)
                }
                history.add(step)
            }

            suspend fun fail(error: DomainException): LocusResult.Failure {
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            // 1. Identify Stack
            val step1 = "Validating bucket ownership..."
            updateStep(step1)

            val tagsResult = s3Client.getBucketTags(creds, bucketName)
            if (tagsResult is LocusResult.Failure) {
                return fail(DomainException.RecoveryError.MissingStackTag)
            }
            val tags =
                (tagsResult as? LocusResult.Success)?.data ?: emptyMap()
            val oldStackName = tags["aws:cloudformation:stack-name"]
            if (oldStackName == null || oldStackName.isBlank()) {
                return fail(DomainException.RecoveryError.MissingStackTag)
            }
            completeStep("Identified existing stack: $oldStackName")

            // 2. Load Template
            val step2 = "Loading CloudFormation template..."
            updateStep(step2)
            val template =
                try {
                    resourceProvider.getStackTemplate()
                } catch (e: Exception) {
                    return fail(DomainException.ProvisioningError.InvalidConfiguration)
                }
            completeStep(step2)

            // 3. Create New Stack (Recovery)
            // We create a NEW stack for the new device but point it to the EXISTING bucket.
            val newDeviceName = UUID.randomUUID().toString()
            val newStackName = "locus-user-$newDeviceName"

            val stackResult =
                stackProvisioningService.createAndPollStack(
                    creds = creds,
                    stackName = newStackName,
                    template = template,
                    // Reuse bucket
                    parameters = mapOf("BucketName" to bucketName),
                    history = history.toList(),
                )

            val resultData =
                when (stackResult) {
                    is LocusResult.Success -> stackResult.data
                    is LocusResult.Failure -> return stackResult
                }

            completeStep("Deployed Recovery Stack")

            val outputs = resultData.outputs
            val stackId = resultData.stackId

            // 4. Success Handling
            val step4 = "Verifying stack outputs..."
            updateStep(step4)

            val accessKeyId = outputs[OUT_RUNTIME_ACCESS_KEY]
            val secretAccessKey = outputs[OUT_RUNTIME_SECRET_KEY]
            val accountId = ArnUtils.extractAccountId(stackId)

            if (accessKeyId == null || secretAccessKey == null || accountId == null) {
                return fail(DomainException.ProvisioningError.DeploymentFailed("Invalid stack outputs"))
            }
            completeStep(step4)

            val step5 = "Finalizing setup..."
            updateStep(step5)

            // Generate NEW device ID for this installation to avoid split-brain
            val newDeviceId = UUID.randomUUID().toString()
            val newSalt = AuthUtils.generateSalt()

            val initResult = configRepository.initializeIdentity(newDeviceId, newSalt)
            if (initResult is LocusResult.Failure) {
                val error =
                    initResult.error as? DomainException
                        ?: DomainException.AuthError.Generic(initResult.error)
                return fail(error)
            }

            val runtimeCreds =
                RuntimeCredentials(
                    accessKeyId = accessKeyId,
                    secretAccessKey = secretAccessKey,
                    bucketName = bucketName,
                    accountId = accountId,
                    region = creds.region,
                    telemetrySalt = newSalt,
                )

            val promoteResult = authRepository.promoteToRuntimeCredentials(runtimeCreds)
            if (promoteResult is LocusResult.Failure) {
                val error =
                    promoteResult.error as? DomainException
                        ?: DomainException.AuthError.Generic(promoteResult.error)
                return fail(error)
            }

            authRepository.updateProvisioningState(ProvisioningState.Success)
            return LocusResult.Success(Unit)
        }
    }
