package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StackOutputsTest {
    @Test
    fun `has correct properties`() {
        val outputs =
            StackOutputs(
                bucketName = "bucket",
                runtimeAccessKeyId = "key",
                runtimeSecretAccessKey = "secret",
            )
        assertThat(outputs.bucketName).isEqualTo("bucket")
        assertThat(outputs.runtimeAccessKeyId).isEqualTo("key")
        assertThat(outputs.runtimeSecretAccessKey).isEqualTo("secret")
    }

    @Test
    fun `verify equality and hashcode`() {
        val outputs1 = StackOutputs("b", "k", "s")
        val outputs2 = StackOutputs("b", "k", "s")
        val outputs3 = StackOutputs("other", "k", "s")

        assertThat(outputs1).isEqualTo(outputs2)
        assertThat(outputs1.hashCode()).isEqualTo(outputs2.hashCode())
        assertThat(outputs1).isNotEqualTo(outputs3)
    }

    @Test
    fun `verify toString`() {
        val outputs = StackOutputs("bucket", "key", "secret")
        assertThat(outputs.toString()).contains("bucket")
        assertThat(outputs.toString()).contains("key")
        assertThat(outputs.toString()).contains("secret")
    }
}
