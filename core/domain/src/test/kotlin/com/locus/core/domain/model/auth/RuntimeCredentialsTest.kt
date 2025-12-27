package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RuntimeCredentialsTest {
    @Test
    fun `has correct properties`() {
        val creds =
            RuntimeCredentials(
                accessKeyId = "access",
                secretAccessKey = "secret",
                bucketName = "my-bucket",
                region = "us-east-1",
                accountId = "123456789012",
                telemetrySalt = "salt",
            )
        assertThat(creds.accessKeyId).isEqualTo("access")
        assertThat(creds.secretAccessKey).isEqualTo("secret")
        assertThat(creds.bucketName).isEqualTo("my-bucket")
        assertThat(creds.region).isEqualTo("us-east-1")
        assertThat(creds.accountId).isEqualTo("123456789012")
        assertThat(creds.telemetrySalt).isEqualTo("salt")
    }

    @Test
    fun `telemetry salt is optional`() {
        val creds =
            RuntimeCredentials(
                accessKeyId = "access",
                secretAccessKey = "secret",
                bucketName = "my-bucket",
                region = "us-east-1",
                accountId = "123456789012",
            )
        assertThat(creds.telemetrySalt).isNull()
    }
}
