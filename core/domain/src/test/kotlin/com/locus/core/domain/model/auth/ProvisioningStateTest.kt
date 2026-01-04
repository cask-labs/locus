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
    fun `Working state holds message and history`() {
        val history = listOf("Step 1", "Step 2")
        val state = ProvisioningState.Working("Step 3", history)
        assertThat(state.currentStep).isEqualTo("Step 3")
        assertThat(state.history).containsExactly("Step 1", "Step 2")
    }

    @Test
    fun `Success state exists`() {
        assertThat(ProvisioningState.Success).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `Failure state holds error and history`() {
        val error = DomainException.NetworkError.Offline
        val history = listOf("Step 1", "Step 2")
        val state = ProvisioningState.Failure(error, null, history)
        assertThat(state.error).isEqualTo(error)
        assertThat(state.history).containsExactly("Step 1", "Step 2")
    }

    @Test
    fun `Failure state has empty history by default`() {
        val error = DomainException.NetworkError.Offline
        val state = ProvisioningState.Failure(error)
        assertThat(state.error).isEqualTo(error)
        assertThat(state.history).isEmpty()
    }

    @Test
    fun `Constant MAX_HISTORY_SIZE is defined`() {
        assertThat(ProvisioningState.MAX_HISTORY_SIZE).isEqualTo(100)
    }
}
