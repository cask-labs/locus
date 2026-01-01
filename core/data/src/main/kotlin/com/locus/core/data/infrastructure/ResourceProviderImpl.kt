package com.locus.core.data.infrastructure

import android.content.Context
import com.locus.core.domain.infrastructure.ResourceProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class ResourceProviderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ResourceProvider {
        override fun getStackTemplate(): String {
            return try {
                context.assets
                    .open("locus-stack.yaml")
                    .use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    }
            } catch (e: Exception) {
                // Fallback or error handling
                throw RuntimeException("Failed to load locus-stack.yaml", e)
            }
        }
    }
