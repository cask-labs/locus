package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthStateTest {
    @Test
    fun `Uninitialized state exists`() {
        val state = AuthState.Uninitialized
        assertThat(state).isInstanceOf(AuthState::class.java)
        assertThat(state).isEqualTo(AuthState.Uninitialized)
        assertThat(state.hashCode()).isEqualTo(AuthState.Uninitialized.hashCode())
        assertThat(state.toString()).isEqualTo("Uninitialized")
    }

    @Test
    fun `SetupPending state exists`() {
        val state = AuthState.SetupPending
        assertThat(state).isInstanceOf(AuthState::class.java)
        assertThat(state).isEqualTo(AuthState.SetupPending)
        assertThat(state.hashCode()).isEqualTo(AuthState.SetupPending.hashCode())
        assertThat(state.toString()).isEqualTo("SetupPending")
    }

    @Test
    fun `Authenticated state exists`() {
        val state = AuthState.Authenticated
        assertThat(state).isInstanceOf(AuthState::class.java)
        assertThat(state).isEqualTo(AuthState.Authenticated)
        assertThat(state.hashCode()).isEqualTo(AuthState.Authenticated.hashCode())
        assertThat(state.toString()).isEqualTo("Authenticated")
    }
}
