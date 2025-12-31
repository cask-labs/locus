package com.locus.core.domain.usecase

import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_ACCESS_KEY
import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_SECRET_KEY
import com.locus.core.domain.infrastructure.InfrastructureConstants.TAG_STACK_NAME
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
            // 1. Resolve Stack Name from Bucket Tags
            authRepository.updateProvisioningState(ProvisioningState.ValidatingBucket)
            val tagsResult = s3Client.getBucketTags(creds, bucketName)
            if (tagsResult is LocusResult.Failure) {
                val error = DomainException.RecoveryError.MissingStackTag
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }
            val tags = (tagsResult as LocusResult.Success).data

            // Validate that this is a Locus-managed bucket by checking for the stack name tag
            if (!tags.containsKey(TAG_STACK_NAME)) {
                val error = DomainException.RecoveryError.MissingStackTag
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            // 2. Load Template
            val template =
                try {
                    resourceProvider.getStackTemplate()
                } catch (e: Exception) {
                    return LocusResult.Failure(DomainException.ProvisioningError.InvalidConfiguration)
                }

            val newDeviceId = UUID.randomUUID().toString()
            val stackNameForRecovery = "locus-user-$newDeviceId"

            // 3. Create Stack and Poll
            val stackResult =
                stackProvisioningService.createAndPollStack(
                    creds = creds,
                    stackName = stackNameForRecovery,
                    template = template,
                    parameters =
                        mapOf(
                            "BucketName" to bucketName,
                            "StackName" to newDeviceId,
                        ),
                )

            val resultData =
                when (stackResult) {
                    is LocusResult.Success -> stackResult.data
                    is LocusResult.Failure -> return stackResult
                }

            val outputs = resultData.outputs
            val stackId = resultData.stackId

            val accessKeyId = outputs[OUT_RUNTIME_ACCESS_KEY]
            val secretAccessKey = outputs[OUT_RUNTIME_SECRET_KEY]
            val accountId = ArnUtils.extractAccountId(stackId)

            if (accessKeyId == null || secretAccessKey == null || accountId == null) {
                val error = DomainException.ProvisioningError.DeploymentFailed("Invalid stack outputs")
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            authRepository.updateProvisioningState(ProvisioningState.FinalizingSetup)

            val newSalt = AuthUtils.generateSalt()
            val initResult = configRepository.initializeIdentity(newDeviceId, newSalt)
            if (initResult is LocusResult.Failure) {
                val error =
                    initResult.error as? DomainException
                        ?: DomainException.AuthError.Generic(initResult.error)
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return initResult as LocusResult.Failure
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
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return promoteResult
            }

            return LocusResult.Success(Unit)
        }
    }
