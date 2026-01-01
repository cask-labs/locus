package com.locus.android

import android.app.Application
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
class LocusApp : Application() {
    @Inject
    lateinit var authRepository: AuthRepository

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
        applicationScope.launch {
            authRepository.initialize()
        }
    }
}
