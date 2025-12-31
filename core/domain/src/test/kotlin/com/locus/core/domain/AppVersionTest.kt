package com.locus.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppVersionTest {
    @Test
    fun `has correct properties`() {
        val version = AppVersion("1.0.0", 1)
        assertThat(version.versionName).isEqualTo("1.0.0")
        assertThat(version.versionCode).isEqualTo(1)
    }

    @Test
    fun `verify equality and hashcode`() {
        val v1 = AppVersion("1.0.0", 1)
        val v2 = AppVersion("1.0.0", 1)
        val v3 = AppVersion("1.0.1", 1)

        assertThat(v1).isEqualTo(v2)
        assertThat(v1.hashCode()).isEqualTo(v2.hashCode())
        assertThat(v1).isNotEqualTo(v3)
    }

    @Test
    fun `verify toString`() {
        val v = AppVersion("1.0.0", 1)
        assertThat(v.toString()).contains("1.0.0")
        assertThat(v.toString()).contains("1")
    }
}
