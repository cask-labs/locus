package com.locus.core.domain.usecase

import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.util.AuthUtils
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject

class ProvisioningUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val configRepository: ConfigurationRepository,
        private val cloudFormationClient: CloudFormationClient,
        private val resourceProvider: ResourceProvider,
    ) {
        suspend operator fun invoke(
            creds: BootstrapCredentials,
            deviceName: String,
        ): LocusResult<Unit> {
            // 1. Validate Input (Basic check, maybe better in VM)
            if (deviceName.isBlank()) {
                return LocusResult.Failure(DomainException.AuthError.InvalidCredentials) // Or InvalidInput
            }

            // 2. Load Template
            val template =
                try {
                    resourceProvider.getStackTemplate()
                } catch (e: Exception) {
                    return LocusResult.Failure(DomainException.NetworkError.Generic(e))
                }

            // 3. Create Stack
            val stackName = "locus-user-$deviceName"
            authRepository.updateProvisioningState(ProvisioningState.DeployingStack(stackName))

            val createResult =
                cloudFormationClient.createStack(
                    creds = creds,
                    stackName = stackName,
                    template = template,
                    parameters = mapOf("StackName" to deviceName),
                )

            if (createResult is LocusResult.Failure) {
                val error =
                    createResult.error as? DomainException
                        ?: DomainException.ProvisioningError.DeploymentFailed(
                            createResult.error.message ?: "Unknown error",
                        )
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return createResult as LocusResult.Failure
            }

            // 4. Poll for Completion
            val startTime = System.currentTimeMillis()
            var success = false
            var outputs: Map<String, String>? = null
            var stackId: String? = null

            while (System.currentTimeMillis() - startTime < POLL_TIMEOUT) {
                val describeResult = cloudFormationClient.describeStack(creds, stackName)
                if (describeResult is LocusResult.Success) {
                    val details = describeResult.data
                    stackId = details.stackId

                    // Update detailed status
                    authRepository.updateProvisioningState(ProvisioningState.WaitingForCompletion(stackName, details.status))

                    when (details.status) {
                        "CREATE_COMPLETE" -> {
                            success = true
                            outputs = details.outputs
                            break
                        }
                        "CREATE_FAILED", "ROLLBACK_IN_PROGRESS", "ROLLBACK_COMPLETE" -> {
                            val error = DomainException.ProvisioningError.DeploymentFailed("Stack creation failed: ${details.status}")
                            authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                            return LocusResult.Failure(error)
                        }
                        // CREATE_IN_PROGRESS, etc.
                    }
                }
                delay(POLL_INTERVAL)
            }

            if (!success) {
                val error = DomainException.NetworkError.Timeout()
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            // 5. Success Handling
            if (outputs == null) {
                val error = DomainException.ProvisioningError.DeploymentFailed("Missing stack outputs")
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            val accessKeyId = outputs[OUT_RUNTIME_ACCESS_KEY]
            val secretAccessKey = outputs[OUT_RUNTIME_SECRET_KEY]
            val bucket = outputs[OUT_BUCKET_NAME]
            val accountId = stackId?.split(":")?.getOrNull(4)

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

        companion object {
            private const val POLL_INTERVAL = 5_000L
            private const val POLL_TIMEOUT = 600_000L // 10 minutes

            private const val OUT_RUNTIME_ACCESS_KEY = "RuntimeAccessKeyId"
            private const val OUT_RUNTIME_SECRET_KEY = "RuntimeSecretAccessKey"
            private const val OUT_BUCKET_NAME = "BucketName"
        }
    }
