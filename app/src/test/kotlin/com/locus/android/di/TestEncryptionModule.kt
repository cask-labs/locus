package com.locus.android.di

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
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
        // Fallback to a cleartext keyset for testing purposes only
        return com.google.crypto.tink.KeysetHandle.generateNew(
            KeyTemplates.get("AES256_GCM"),
        ).getPrimitive(Aead::class.java)
    }
}
