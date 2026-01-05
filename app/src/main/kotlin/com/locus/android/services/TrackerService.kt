package com.locus.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.locus.android.R
import com.locus.android.util.NotificationConstants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (!hasRequiredPermissions()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundSafely()

        return START_STICKY
    }

    private fun startForegroundSafely() {
        try {
            // Enforce foreground immediately to avoid Android 12+ / 14+ crashes
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: SecurityException) {
            // Fallback safety for Android 14+ strict rules
            android.util.Log.e("TrackerService", "Failed to start foreground", e)
            stopSelf()
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // Catch strict mode exceptions (e.g. ForegroundServiceStartNotAllowedException on Android 12+)
            // safely without causing VerifyErrors on older Android versions.
            android.util.Log.e("TrackerService", "Foreground start failed", e)
            stopSelf()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Prior to Android 14, system might be more lenient, or we assume verified by UI
        }
    }

    private fun createNotification(): Notification {
        val channelId = NotificationConstants.CHANNEL_ID_TRACKING
        val channelName = getString(R.string.notification_channel_tracking_name)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW,
                )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText(getString(R.string.notification_tracking_text))
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this resource exists or use a default android one
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1337

        fun start(context: Context) {
            val intent = Intent(context, TrackerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
