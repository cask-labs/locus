package com.locus.core.domain.usecase

import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_BUCKET_NAME
import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_ACCESS_KEY
import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_SECRET_KEY
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.StackProvisioningService
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.util.AuthUtils
import java.util.UUID
import javax.inject.Inject

class ProvisioningUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val configRepository: ConfigurationRepository,
        private val resourceProvider: ResourceProvider,
        private val stackProvisioningService: StackProvisioningService,
    ) {
        suspend operator fun invoke(
            creds: BootstrapCredentials,
            deviceName: String,
        ): LocusResult<Unit> {
            // 1. Validate Input
            if (deviceName.isBlank()) {
                return LocusResult.Failure(DomainException.AuthError.InvalidCredentials)
            }

            // 2. Load Template
            val template =
                try {
                    resourceProvider.getStackTemplate()
                } catch (e: Exception) {
                    return LocusResult.Failure(DomainException.ProvisioningError.InvalidConfiguration)
                }

            // 3. Create Stack and Poll
            val stackName = "locus-user-$deviceName"

            val stackResult =
                stackProvisioningService.createAndPollStack(
                    creds = creds,
                    stackName = stackName,
                    template = template,
                    parameters = mapOf("StackName" to deviceName),
                )

            val resultData =
                when (stackResult) {
                    is LocusResult.Success -> stackResult.data
                    is LocusResult.Failure -> return stackResult
                }

            val outputs = resultData.outputs
            val stackId = resultData.stackId

            // 4. Success Handling
            val accessKeyId = outputs[OUT_RUNTIME_ACCESS_KEY]
            val secretAccessKey = outputs[OUT_RUNTIME_SECRET_KEY]
            val bucket = outputs[OUT_BUCKET_NAME]
            val accountId = stackId.split(":").getOrNull(4)

            if (accessKeyId == null || secretAccessKey == null || bucket == null || accountId == null) {
                val error = DomainException.ProvisioningError.DeploymentFailed("Invalid stack outputs")
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            authRepository.updateProvisioningState(ProvisioningState.FinalizingSetup)

            val newDeviceId = UUID.randomUUID().toString()
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
                    bucketName = bucket,
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
