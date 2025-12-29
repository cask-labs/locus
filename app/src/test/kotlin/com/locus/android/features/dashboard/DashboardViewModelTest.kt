package com.locus.android.features.dashboard

import app.cash.turbine.test
import com.locus.core.domain.AppVersion
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.GetAppVersionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val getAppVersionUseCase: GetAppVersionUseCase = mockk()
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits Success when use case returns success`() =
        runTest(testDispatcher) {
            val appVersion = AppVersion("1.0.0", 1)
            coEvery { getAppVersionUseCase() } returns LocusResult.Success(appVersion)

            viewModel = DashboardViewModel(getAppVersionUseCase)

            viewModel.uiState.test {
                // Initial state might be null
                assertEquals(null, awaitItem())

                // Then Success
                val item = awaitItem()
                assert(item is LocusResult.Success<*>)
                assertEquals("1.0.0", (item as LocusResult.Success<AppVersion>).data.versionName)
            }
        }

    @Test
    fun `uiState emits Failure when use case returns failure`() =
        runTest(testDispatcher) {
            val exception = Exception("Test error")
            coEvery { getAppVersionUseCase() } returns LocusResult.Failure(exception)

            viewModel = DashboardViewModel(getAppVersionUseCase)

            viewModel.uiState.test {
                assertEquals(null, awaitItem())

                val item = awaitItem()
                assert(item is LocusResult.Failure)
                assertEquals("Test error", (item as LocusResult.Failure).error.message)
            }
        }
}
