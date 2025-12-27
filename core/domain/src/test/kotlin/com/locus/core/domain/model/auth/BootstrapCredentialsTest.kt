package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BootstrapCredentialsTest {
    @Test
    fun `has correct properties`() {
        val creds = BootstrapCredentials("access", "secret", "token")
        assertThat(creds.accessKeyId).isEqualTo("access")
        assertThat(creds.secretAccessKey).isEqualTo("secret")
        assertThat(creds.sessionToken).isEqualTo("token")
    }
}
