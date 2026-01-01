package com.locus.core.domain.util

import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun currentTimeMillis(): Long

    suspend fun delay(timeMillis: Long)
}

@Singleton
class DefaultTimeProvider
    @Inject
    constructor() : TimeProvider {
        override fun currentTimeMillis(): Long = System.currentTimeMillis()

        override suspend fun delay(timeMillis: Long) {
            kotlinx.coroutines.delay(timeMillis)
        }
    }
