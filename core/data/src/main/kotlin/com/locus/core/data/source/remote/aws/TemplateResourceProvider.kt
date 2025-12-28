package com.locus.core.data.source.remote.aws

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

interface TemplateResourceProvider {
    fun loadTemplate(): String
}

class AssetTemplateResourceProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TemplateResourceProvider {
        override fun loadTemplate(): String {
            return context.assets.open("locus-stack.yaml").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        }
    }
