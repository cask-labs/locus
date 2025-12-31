package com.locus.core.domain.infrastructure

import com.locus.core.domain.infrastructure.InfrastructureConstants.PERMANENT_ERROR_STATUSES
import com.locus.core.domain.infrastructure.InfrastructureConstants.POLL_INTERVAL
import com.locus.core.domain.infrastructure.InfrastructureConstants.POLL_TIMEOUT
import com.locus.core.domain.infrastructure.InfrastructureConstants.STATUS_CREATE_COMPLETE
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.StackProvisioningResult
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.util.TimeProvider
import javax.inject.Inject

/**
 * Domain service to handle CloudFormation stack provisioning and polling.
 * Encapsulates the logic for creating stacks, polling for completion, and handling
 * "Fail Fast" scenarios for permanent errors.
 */
class StackProvisioningService
    @Inject
    constructor(
        private val cloudFormationClient: CloudFormationClient,
        private val authRepository: AuthRepository,
        private val timeProvider: TimeProvider,
    ) {
        /**
         * Creates a CloudFormation stack and polls for its completion.
         *
         * @param creds The bootstrap credentials to use.
         * @param stackName The name of the stack to create.
         * @param template The CloudFormation template body.
         * @param parameters The parameters for the stack.
         * @return A [LocusResult] containing the stack ID and outputs on success.
         */
        suspend fun createAndPollStack(
            creds: BootstrapCredentials,
            stackName: String,
            template: String,
            parameters: Map<String, String>,
        ): LocusResult<StackProvisioningResult> {
            authRepository.updateProvisioningState(ProvisioningState.DeployingStack(stackName))

            // 1. Create Stack
            val createResult =
                cloudFormationClient.createStack(
                    creds = creds,
                    stackName = stackName,
                    template = template,
                    parameters = parameters,
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

            // 2. Poll for Completion
            return pollForCompletion(creds, stackName)
        }

        private suspend fun pollForCompletion(
            creds: BootstrapCredentials,
            stackName: String,
        ): LocusResult<StackProvisioningResult> {
            val startTime = timeProvider.currentTimeMillis()

            while (timeProvider.currentTimeMillis() - startTime < POLL_TIMEOUT) {
                val describeResult = cloudFormationClient.describeStack(creds, stackName)

                if (describeResult is LocusResult.Success) {
                    val details = describeResult.data

                    authRepository.updateProvisioningState(
                        ProvisioningState.WaitingForCompletion(stackName, details.status),
                    )

                    if (details.status == STATUS_CREATE_COMPLETE) {
                        if (details.outputs == null) {
                            return fail(DomainException.ProvisioningError.DeploymentFailed("Missing stack outputs"))
                        }
                        return LocusResult.Success(StackProvisioningResult(details.stackId ?: "", details.outputs))
                    }

                    if (PERMANENT_ERROR_STATUSES.contains(details.status)) {
                        return fail(
                            DomainException.ProvisioningError.DeploymentFailed(
                                "Stack creation failed: ${details.status}",
                            ),
                        )
                    }

                    // If status is not complete or failed (e.g., CREATE_IN_PROGRESS), continue polling
                } else if (describeResult is LocusResult.Failure) {
                    // Check if it's a permanent error (Fail Fast)
                    val error = describeResult.error
                    if (isPermanentError(error)) {
                        return fail(
                            DomainException.ProvisioningError.DeploymentFailed(
                                "Permanent error during polling: ${error.message}",
                            ),
                        )
                    }
                    // If transient, just log (implicitly) and continue retrying
                }

                timeProvider.delay(POLL_INTERVAL)
            }

            return fail(DomainException.NetworkError.Timeout())
        }

        private suspend fun fail(error: DomainException): LocusResult.Failure {
            authRepository.updateProvisioningState(ProvisioningState.Failure(error))
            return LocusResult.Failure(error)
        }

        private fun isPermanentError(error: Throwable): Boolean {
            return when (error) {
                is DomainException.AuthError -> true
                is DomainException.ProvisioningError.Quota -> true
                is DomainException.ProvisioningError.Permissions -> true
                else -> false
            }
        }
    }
