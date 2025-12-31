package com.locus.android

import android.app.Application
import android.util.Log
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LocusApp : Application() {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                authRepository.initialize()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("LocusApp", "Failed to initialize AuthRepository", e)
            }
        }
    }
}
