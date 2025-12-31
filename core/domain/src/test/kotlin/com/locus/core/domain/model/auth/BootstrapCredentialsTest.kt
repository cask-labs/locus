package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BootstrapCredentialsTest {
    @Test
    fun `has correct properties`() {
        val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
        assertThat(creds.accessKeyId).isEqualTo("access")
        assertThat(creds.secretAccessKey).isEqualTo("secret")
        assertThat(creds.sessionToken).isEqualTo("token")
        assertThat(creds.region).isEqualTo("us-east-1")
    }

    @Test
    fun `verify equality and hashcode`() {
        val creds1 = BootstrapCredentials("id", "secret", "token", "region")
        val creds2 = BootstrapCredentials("id", "secret", "token", "region")
        val creds3 = BootstrapCredentials("id2", "secret", "token", "region")

        assertThat(creds1).isEqualTo(creds2)
        assertThat(creds1.hashCode()).isEqualTo(creds2.hashCode())
        assertThat(creds1).isNotEqualTo(creds3)
    }

    @Test
    fun `verify toString`() {
        val creds = BootstrapCredentials("id", "secret", "token", "region")
        assertThat(creds.toString()).contains("id")
        assertThat(creds.toString()).contains("secret")
    }
}
