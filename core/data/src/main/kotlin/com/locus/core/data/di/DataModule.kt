package com.locus.core.data.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.locus.core.data.model.BootstrapCredentialsDto
import com.locus.core.data.model.RuntimeCredentialsDto
import com.locus.core.data.repository.AppVersionRepositoryImpl
import com.locus.core.data.repository.AuthRepositoryImpl
import com.locus.core.data.source.local.EncryptedDataStoreSerializer
import com.locus.core.domain.repository.AppVersionRepository
import com.locus.core.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.nullable
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindAppVersionRepository(appVersionRepositoryImpl: AppVersionRepositoryImpl): AppVersionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    companion object {
        private const val MASTER_KEY_URI = "android-keystore://master_key"
        private const val KEYSET_NAME = "locus_keyset"
        private const val PREF_FILE_NAME = "locus_master_key_preference"

        @Provides
        @Singleton
        fun provideAead(
            @ApplicationContext context: Context,
        ): Aead {
            return AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)
        }

        @Provides
        @Singleton
        @Named("bootstrapDataStore")
        fun provideBootstrapDataStore(
            @ApplicationContext context: Context,
            aead: Aead,
        ): DataStore<BootstrapCredentialsDto?> {
            return DataStoreFactory.create(
                serializer =
                    EncryptedDataStoreSerializer(
                        aead = aead,
                        serializer = BootstrapCredentialsDto.serializer().nullable,
                        defaultValueProvider = { null },
                        associatedData = "bootstrap_creds.pb",
                    ),
                produceFile = { context.dataStoreFile("bootstrap_creds.pb") },
            )
        }

        @Provides
        @Singleton
        @Named("runtimeDataStore")
        fun provideRuntimeDataStore(
            @ApplicationContext context: Context,
            aead: Aead,
        ): DataStore<RuntimeCredentialsDto?> {
            return DataStoreFactory.create(
                serializer =
                    EncryptedDataStoreSerializer(
                        aead = aead,
                        serializer = RuntimeCredentialsDto.serializer().nullable,
                        defaultValueProvider = { null },
                        associatedData = "runtime_creds.pb",
                    ),
                produceFile = { context.dataStoreFile("runtime_creds.pb") },
            )
        }

        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context,
        ): SharedPreferences {
            return context.getSharedPreferences("locus_settings", Context.MODE_PRIVATE)
        }
    }
}
