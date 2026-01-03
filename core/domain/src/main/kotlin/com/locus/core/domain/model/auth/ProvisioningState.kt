package com.locus.core.domain.model.auth

import com.locus.core.domain.result.DomainException

/**
 * Detailed state machine for the CloudFormation provisioning process.
 */
sealed class ProvisioningState {
    data object Idle : ProvisioningState()

    data class Working(
        val currentStep: String,
        val history: List<String> = emptyList(),
    ) : ProvisioningState()

    data object Success : ProvisioningState()

    data class Failure(val error: DomainException) : ProvisioningState()

    companion object {
        const val MAX_PROVISIONING_HISTORY_SIZE = 100
    }
}
