package com.locus.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.locus.core.domain.AppVersion
import com.locus.core.domain.repository.AppVersionRepository
import com.locus.core.domain.result.LocusResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppVersionRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AppVersionRepository {
        override suspend fun getAppVersion(): LocusResult<AppVersion> {
            return try {
                val packageInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }

                // Min SDK is 28, so longVersionCode is available.
                val code = packageInfo.longVersionCode.toInt()

                LocusResult.Success(
                    AppVersion(
                        versionName = packageInfo.versionName ?: "Unknown",
                        versionCode = code,
                    ),
                )
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }
    }
