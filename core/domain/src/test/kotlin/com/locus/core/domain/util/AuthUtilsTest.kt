package com.locus.core.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthUtilsTest {
    @Test
    fun generateSalt_returns64CharacterHexString() {
        val salt = AuthUtils.generateSalt()
        assertThat(salt).matches("^[0-9a-f]{64}$")
    }

    @Test
    fun generateSalt_returnsUniqueValues() {
        val salt1 = AuthUtils.generateSalt()
        val salt2 = AuthUtils.generateSalt()
        assertThat(salt1).isNotEqualTo(salt2)
    }
}
