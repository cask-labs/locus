package com.locus.android.di

import android.content.Context
import com.locus.android.services.TrackerService
import com.locus.core.domain.usecase.TrackingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppServiceModule {

    @Provides
    @Singleton
    fun provideTrackingManager(@ApplicationContext context: Context): TrackingManager {
        return object : TrackingManager {
            override fun startTracking() {
                TrackerService.start(context)
            }
        }
    }
}
