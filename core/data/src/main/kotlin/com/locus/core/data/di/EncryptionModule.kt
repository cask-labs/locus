package com.locus.core.data.di

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EncryptionModule {
    private const val MASTER_KEY_URI = "android-keystore://master_key"
    private const val KEYSET_NAME = "locus_keyset"
    private const val PREF_FILE_NAME = "locus_master_key_preference"

    @Provides
    @Singleton
    fun provideAead(
        @ApplicationContext context: Context,
    ): Aead {
        return try {
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)
        } catch (e: Exception) {
            // If running in unit tests (Robolectric or local), return a dummy AEAD
            // This checks if we are likely in a test env where KeyStore is failing
            if (isRobolectric()) {
                // Note: We intentionally avoid relying on build-type flags here so that we can
                // run unit tests against Release builds (e.g. testReleaseUnitTest).
                // The isRobolectric() check is sufficient safety because org.robolectric.Robolectric
                // class will not be present in the production APK runtime.
                //
                // IMPORTANT: This creates an INSECURE AEAD implementation that performs no real
                // encryption or authentication and must NEVER be used in production code.
                return object : Aead {
                    override fun encrypt(
                        plaintext: ByteArray,
                        associatedData: ByteArray?,
                    ): ByteArray {
                        return plaintext // No-op encryption for tests
                    }

                    override fun decrypt(
                        ciphertext: ByteArray,
                        associatedData: ByteArray?,
                    ): ByteArray {
                        return ciphertext // No-op decryption for tests
                    }
                }
            }
            // In production, we must Fail Hard if secure storage is unavailable.
            // Do NOT fallback to insecure storage.
            throw e
        }
    }

    private fun isRobolectric(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
