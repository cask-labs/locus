package com.locus.core.data.source.remote.aws

import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import aws.sdk.kotlin.services.s3.S3Client as AwsS3Client

class S3ClientTest {
    private val credentialsProvider = mockk<LocusCredentialsProvider>()
    private val awsClientFactory = mockk<AwsClientFactory>()
    private val awsS3Client = mockk<AwsS3Client>(relaxed = true)

    private val client = S3Client(credentialsProvider, awsClientFactory)

    @Test
    fun `uploadTrack returns success when S3 putObject succeeds`() =
        runTest {
            // Mock Factory
            every { awsClientFactory.createS3Client(any()) } returns awsS3Client

            // Mock S3 Client behavior
            coEvery { awsS3Client.putObject(any()) } returns PutObjectResponse {}

            val result = client.uploadTrack("test-bucket", "test-key", byteArrayOf(1, 2, 3))

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)

            // Verify interaction
            val slot = slot<PutObjectRequest>()
            coVerify { awsS3Client.putObject(capture(slot)) }

            assertThat(slot.captured.bucket).isEqualTo("test-bucket")
            assertThat(slot.captured.key).isEqualTo("test-key")
        }

    @Test
    fun `uploadTrack returns failure when S3 client throws`() =
        runTest {
            every { awsClientFactory.createS3Client(any()) } returns awsS3Client
            coEvery { awsS3Client.putObject(any()) } throws RuntimeException("Network Error")

            val result = client.uploadTrack("test-bucket", "test-key", byteArrayOf())

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
        }
}
