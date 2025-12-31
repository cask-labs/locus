package com.locus.core.data.di

import android.content.Context
import android.content.SharedPreferences
import com.locus.core.data.infrastructure.CloudFormationClientImpl
import com.locus.core.data.infrastructure.ResourceProviderImpl
import com.locus.core.data.infrastructure.S3ClientImpl
import com.locus.core.data.repository.AppVersionRepositoryImpl
import com.locus.core.data.repository.AuthRepositoryImpl
import com.locus.core.data.repository.ConfigurationRepositoryImpl
import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.S3Client
import com.locus.core.domain.repository.AppVersionRepository
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindAppVersionRepository(appVersionRepositoryImpl: AppVersionRepositoryImpl): AppVersionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindConfigurationRepository(configurationRepositoryImpl: ConfigurationRepositoryImpl): ConfigurationRepository

    @Binds
    abstract fun bindCloudFormationClient(cloudFormationClientImpl: CloudFormationClientImpl): CloudFormationClient

    @Binds
    abstract fun bindS3Client(s3ClientImpl: S3ClientImpl): S3Client

    @Binds
    abstract fun bindResourceProvider(resourceProviderImpl: ResourceProviderImpl): ResourceProvider

    companion object {
        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context,
        ): SharedPreferences {
            return context.getSharedPreferences("locus_settings", Context.MODE_PRIVATE)
        }

        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    }
}
