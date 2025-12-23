package com.locus.core.data.di

import com.locus.core.data.repository.AppVersionRepositoryImpl
import com.locus.core.domain.repository.AppVersionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindAppVersionRepository(impl: AppVersionRepositoryImpl): AppVersionRepository
}
