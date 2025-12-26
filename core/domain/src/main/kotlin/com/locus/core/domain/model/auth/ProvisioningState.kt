package com.locus.core.domain.model.auth

import com.locus.core.domain.result.DomainException

/**
 * Detailed state machine for the CloudFormation provisioning process.
 */
sealed class ProvisioningState {
    data object Idle : ProvisioningState()

    data object ValidatingInput : ProvisioningState()

    data object VerifyingBootstrapKeys : ProvisioningState()

    data class DeployingStack(val stackName: String) : ProvisioningState()

    data class WaitingForCompletion(val stackName: String, val status: String) : ProvisioningState()

    data object FinalizingSetup : ProvisioningState()

    data object Success : ProvisioningState()

    data class Failure(val error: DomainException) : ProvisioningState()
}
