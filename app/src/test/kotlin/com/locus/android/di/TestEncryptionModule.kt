package com.locus.android.di

import com.google.crypto.tink.Aead
import com.locus.core.data.di.EncryptionModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [EncryptionModule::class],
)
object TestEncryptionModule {
    @Provides
    @Singleton
    fun provideAead(): Aead {
        return FakeAead()
    }

    private class FakeAead : Aead {
        override fun encrypt(
            plaintext: ByteArray,
            associatedData: ByteArray,
        ): ByteArray {
            return plaintext
        }

        override fun decrypt(
            ciphertext: ByteArray,
            associatedData: ByteArray,
        ): ByteArray {
            return ciphertext
        }
    }
}
