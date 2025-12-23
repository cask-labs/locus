package com.locus.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.locus.core.data.repository.AppVersionRepositoryImpl
import com.locus.core.domain.LocusResult
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppVersionRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var repository: AppVersionRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = AppVersionRepositoryImpl(context)
    }

    @Test
    fun `returns app version from package manager`() =
        runTest {
            val result = repository.getAppVersion()

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val data = (result as LocusResult.Success).data
            assertThat(data.versionName).isNotNull()
        }
}
