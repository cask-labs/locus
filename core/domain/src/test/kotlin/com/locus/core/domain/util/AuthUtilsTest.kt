package com.locus.core.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthUtilsTest {
    @Test
    fun `generateSalt returns 64 character hex string`() {
        val salt = AuthUtils.generateSalt()
        assertThat(salt).hasLength(64)
        assertThat(salt).matches("^[0-9a-fA-F]+$")
        // Ensure it's not just empty/zeros (kills mutation that removes random.nextBytes)
        assertThat(salt).isNotEqualTo("0".repeat(64))
    }

    @Test
    fun `generateSalt returns unique values`() {
        val salt1 = AuthUtils.generateSalt()
        val salt2 = AuthUtils.generateSalt()
        assertThat(salt1).isNotEqualTo(salt2)
    }
}
