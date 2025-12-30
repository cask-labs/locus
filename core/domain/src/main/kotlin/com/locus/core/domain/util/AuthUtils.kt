package com.locus.core.domain.util

import java.security.SecureRandom

object AuthUtils {
    fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}
