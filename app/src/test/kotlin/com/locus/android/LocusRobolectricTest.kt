package com.locus.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LocusRobolectricTest {
    @Test
    fun `application context is available`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThat(context).isNotNull()
        assertThat(context.packageName).isEqualTo("com.locus.android")
    }
}
