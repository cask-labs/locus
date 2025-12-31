package com.locus.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.locus.core.data.di.AeadModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Singleton

@HiltAndroidTest
@Config(application = HiltTestApplication::class, sdk = [34])
@UninstallModules(AeadModule::class)
@RunWith(RobolectricTestRunner::class)
class LocusRobolectricTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun `application context is available`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThat(context).isNotNull()
        assertThat(context.packageName).isEqualTo("com.locus.android")
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestAeadModule {
        @Provides
        @Singleton
        fun provideAead(): Aead {
            // Safe replacement for tests - Generates a cleartext keyset in memory
            return KeysetHandle.generateNew(
                KeyTemplates.get("AES256_GCM")
            ).getPrimitive(Aead::class.java)
        }
    }
}
