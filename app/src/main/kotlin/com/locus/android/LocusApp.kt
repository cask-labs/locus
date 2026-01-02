package com.locus.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.crypto.tink.config.TinkConfig
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException
import javax.inject.Inject

@HiltAndroidApp
class LocusApp : Application(), Configuration.Provider {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Register Tink configs globally early, before Hilt injection
        try {
            TinkConfig.register()
        } catch (
            @Suppress("SwallowedException") e: GeneralSecurityException,
        ) {
            // Should not happen in normal runtime, but safe to ignore if already registered
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        applicationScope.launch {
            authRepository.initialize()
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Setup Channel
        val setupChannel =
            NotificationChannel(
                "setup_status",
                "Setup Status",
                NotificationManager.IMPORTANCE_LOW,
            )
        notificationManager.createNotificationChannel(setupChannel)

        // Tracking Channel
        val trackingChannel =
            NotificationChannel(
                "tracking_status",
                "Tracking Status",
                NotificationManager.IMPORTANCE_LOW,
            )
        notificationManager.createNotificationChannel(trackingChannel)
    }
}
