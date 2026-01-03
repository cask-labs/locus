package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.result.DomainException
import org.junit.Test

class ProvisioningStateTest {
    @Test
    fun `Idle state exists`() {
        assertThat(ProvisioningState.Idle).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `Working state holds step and history`() {
        val history = listOf("Step 1", "Step 2")
        val state = ProvisioningState.Working("Step 3", history)
        assertThat(state.currentStep).isEqualTo("Step 3")
        assertThat(state.history).containsExactlyElementsIn(history)
    }

    @Test
    fun `Success state exists`() {
        assertThat(ProvisioningState.Success).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `Failure state holds error`() {
        val error = DomainException.NetworkError.Offline
        val state = ProvisioningState.Failure(error)
        assertThat(state.error).isEqualTo(error)
    }

    @Test
    fun `Max history size is 100`() {
        assertThat(ProvisioningState.MAX_HISTORY_SIZE).isEqualTo(100)
    }
}
