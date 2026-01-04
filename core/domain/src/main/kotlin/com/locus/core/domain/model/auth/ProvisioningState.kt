package com.locus.core.domain.model.auth

import com.locus.core.domain.result.DomainException

/**
 * Detailed state machine for the CloudFormation provisioning process.
 */
sealed class ProvisioningState(open val history: List<String>) {
    data object Idle : ProvisioningState(emptyList())

    data class Working(
        val currentStep: String,
        override val history: List<String> = emptyList(),
    ) : ProvisioningState(history)

    data class Success(override val history: List<String> = emptyList()) : ProvisioningState(history)

    data class Failure(
        val error: DomainException,
        override val history: List<String> = emptyList(),
    ) : ProvisioningState(history)

    companion object {
        const val MAX_HISTORY_SIZE = 100
    }
}
