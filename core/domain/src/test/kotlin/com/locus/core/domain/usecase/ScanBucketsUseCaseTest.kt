package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.S3Client
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ScanBucketsUseCaseTest {
    private val s3Client = mockk<S3Client>()
    private val useCase = ScanBucketsUseCase(s3Client)
    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")

    @Test
    fun `returns failure when listing buckets fails`() =
        runBlocking {
            val expectedError = DomainException.NetworkError.Generic(Exception("Network Error"))
            coEvery { s3Client.listBuckets(creds) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }

    @Test
    fun `returns valid buckets correctly`() =
        runBlocking {
            // Given
            coEvery { s3Client.listBuckets(creds) } returns
                LocusResult.Success(
                    listOf("locus-bucket-1", "locus-bucket-2", "other-bucket"),
                )

            coEvery { s3Client.getBucketTags(creds, "locus-bucket-1") } returns
                LocusResult.Success(mapOf("LocusRole" to "DeviceBucket"))
            coEvery { s3Client.getBucketTags(creds, "locus-bucket-2") } returns
                LocusResult.Success(mapOf("LocusRole" to "WrongRole"))

            // When
            val result = useCase(creds)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val data = (result as LocusResult.Success).data
            assertThat(data).hasSize(2)
            assertThat(data[0].first).isEqualTo("locus-bucket-1")
            assertThat(data[0].second).isEqualTo(BucketValidationStatus.Available)
            assertThat(data[1].first).isEqualTo("locus-bucket-2")
            assertThat(data[1].second).isInstanceOf(BucketValidationStatus.Invalid::class.java)
        }

    @Test
    fun `handles bucket tag fetch failure gracefully`() =
        runBlocking {
            // Given
            coEvery { s3Client.listBuckets(creds) } returns
                LocusResult.Success(
                    listOf("locus-bucket-1"),
                )

            coEvery { s3Client.getBucketTags(creds, "locus-bucket-1") } returns
                LocusResult.Failure(DomainException.NetworkError.Generic(Exception("Access Denied")))

            // When
            val result = useCase(creds)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val data = (result as LocusResult.Success).data
            assertThat(data).hasSize(1)
            assertThat(data[0].first).isEqualTo("locus-bucket-1")
            assertThat(data[0].second).isInstanceOf(BucketValidationStatus.Invalid::class.java)
            assertThat((data[0].second as BucketValidationStatus.Invalid).reason).contains("Access denied")
        }
}
