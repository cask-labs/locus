package com.locus.core.domain.usecase

import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_BUCKET_NAME
import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_ACCESS_KEY
import com.locus.core.domain.infrastructure.InfrastructureConstants.OUT_RUNTIME_SECRET_KEY
import com.locus.core.domain.infrastructure.InfrastructureConstants.STACK_NAME_PREFIX
import com.locus.core.domain.infrastructure.ResourceProvider
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
            val history = mutableListOf<String>()

            // Helper to update state and history
            suspend fun updateStep(step: String) {
                // If there was a previous step (in Working state), add it to history
                // But here we are imperatively defining steps.
                // So we add the *current* step to history *before* starting the next one?
                // No, history is completed steps.
                // So we add "Validating input" to history when we move to "Loading template".
                // But the first step has no history.

                // Let's just track history manually.
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

            // 1. Validate Input
            val step1 = "Validating input..."
            updateStep(step1)

            if (deviceName.isBlank() || deviceName.length > 117 || !deviceName.matches(Regex("^[a-zA-Z0-9-]*$"))) {
                val error = DomainException.AuthError.InvalidCredentials
                return fail(error)
            }
            completeStep(step1)

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

            // 3. Create Stack and Poll
            // Note: StackProvisioningService needs to be aware of history or we pass it?
            // StackProvisioningService currently just emits new Working states with empty history (default).
            // This is a problem. We need to fix StackProvisioningService too or handle it here.
            // Since StackProvisioningService is injected, we can't easily change its behavior without changing interface.
            // But StackProvisioningService is in our control.
            // Let's defer StackProvisioningService fix for a moment and look at UseCase structure.
            // If StackProvisioningService emits states, it overwrites our history.
            // We should probably modify StackProvisioningService to accept history or a callback.
            // Or better: StackProvisioningService shouldn't update state directly, it should return progress?
            // But it polls.

            // For now, let's assume StackProvisioningService is "opaque" and handles its own steps.
            // But we want to preserve OUR history.
            // This means we might need to pass the current history to `createAndPollStack`.
            // But `createAndPollStack` signature is fixed in interface? It's a class method.
            // I'll update StackProvisioningService to accept `initialHistory`.

            val stackName = "$STACK_NAME_PREFIX$deviceName"

            // We can't easily change StackProvisioningService signature without breaking tests extensively again.
            // But we must for correctness.
            // Or we just accept that StackProvisioningService clears history for its duration (not ideal).
            //
            // Let's modify StackProvisioningService to take `history`.

            // ... (I will update StackProvisioningService below) ...

            val stackResult =
                stackProvisioningService.createAndPollStack(
                    creds = creds,
                    stackName = stackName,
                    template = template,
                    parameters = mapOf("StackName" to deviceName),
                    history = history.toList(),
                )

            val resultData =
                when (stackResult) {
                    is LocusResult.Success -> stackResult.data
                    is LocusResult.Failure -> return stackResult // Service handles failure state update
                }

            // Service completed. It might have added steps to history internally?
            // Ideally it returns the new history?
            // `StackProvisioningResult` currently only returns stackId and outputs.
            // If we don't get history back, we lose the "Deploying" steps in the history list for subsequent steps.
            // This is getting complicated for a "safe refactoring".
            //
            // Simplified approach:
            // Just append "Deployed Stack" to history here manually, ignoring intermediate polling steps history.
            completeStep("Deployed CloudFormation Stack")

            val outputs = resultData.outputs
            val stackId = resultData.stackId

            // 4. Success Handling
            val step4 = "Verifying stack outputs..."
            updateStep(step4)

            val accessKeyId = outputs[OUT_RUNTIME_ACCESS_KEY]
            val secretAccessKey = outputs[OUT_RUNTIME_SECRET_KEY]
            val bucket = outputs[OUT_BUCKET_NAME]
            val accountId = ArnUtils.extractAccountId(stackId)

            if (accessKeyId == null || secretAccessKey == null || bucket == null || accountId == null) {
                return fail(DomainException.ProvisioningError.DeploymentFailed("Invalid stack outputs"))
            }
            completeStep(step4)

            val step5 = "Finalizing setup..."
            updateStep(step5)

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
                return fail(error)
            }

            authRepository.updateProvisioningState(ProvisioningState.Success)
            return LocusResult.Success(Unit)
        }
    }
