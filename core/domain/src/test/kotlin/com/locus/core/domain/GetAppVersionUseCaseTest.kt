package com.locus.core.domain

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.repository.AppVersionRepository
import com.locus.core.domain.usecase.GetAppVersionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetAppVersionUseCaseTest {
    private val repository = mockk<AppVersionRepository>()
    private val useCase = GetAppVersionUseCase(repository)

    @Test
    fun `returns success when repository returns version`() =
        runTest {
            val expectedVersion = AppVersion("1.0", 1)
            coEvery { repository.getAppVersion() } returns LocusResult.Success(expectedVersion)

            val result = useCase()

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            assertThat((result as LocusResult.Success<*>).data).isEqualTo(expectedVersion)
        }

    @Test
    fun `returns failure when repository fails`() =
        runTest {
            val expectedError = RuntimeException("Error")
            coEvery { repository.getAppVersion() } returns LocusResult.Failure(expectedError)

            val result = useCase()

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }
}
