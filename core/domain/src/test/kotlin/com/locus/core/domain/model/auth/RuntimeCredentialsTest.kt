package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RuntimeCredentialsTest {
    @Test
    fun `has correct properties`() {
        val creds = RuntimeCredentials("access", "secret", "token")
        assertThat(creds.accessKeyId).isEqualTo("access")
        assertThat(creds.secretAccessKey).isEqualTo("secret")
        assertThat(creds.sessionToken).isEqualTo("token")
    }

    @Test
    fun `session token is optional`() {
        val creds = RuntimeCredentials("access", "secret")
        assertThat(creds.sessionToken).isNull()
    }
}
