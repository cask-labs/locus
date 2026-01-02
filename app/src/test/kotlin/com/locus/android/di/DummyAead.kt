package com.locus.android.di

import com.google.crypto.tink.Aead

class DummyAead : Aead {
    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        return plaintext // Insecure, but fine for unit tests checking logic
    }

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
        return ciphertext
    }
}
