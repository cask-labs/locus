package com.locus.core.domain

interface AppVersionRepository {
    suspend fun getAppVersion(): LocusResult<AppVersion>
}
