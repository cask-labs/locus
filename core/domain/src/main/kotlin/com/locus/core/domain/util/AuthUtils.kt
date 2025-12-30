package com.locus.core.domain.util

import java.security.SecureRandom
import java.util.HexFormat

object AuthUtils {
    fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return HexFormat.of().formatHex(bytes)
    }
}
