package com.locus.core.data.repository

import app.cash.turbine.test
import aws.sdk.kotlin.services.cloudformation.CloudFormationClient
import aws.sdk.kotlin.services.cloudformation.model.DescribeStacksRequest
import aws.sdk.kotlin.services.cloudformation.model.DescribeStacksResponse
import aws.sdk.kotlin.services.cloudformation.model.Output
import aws.sdk.kotlin.services.cloudformation.model.Stack
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetBucketTaggingRequest
import aws.sdk.kotlin.services.s3.model.GetBucketTaggingResponse
import aws.sdk.kotlin.services.s3.model.HeadBucketRequest
import aws.sdk.kotlin.services.s3.model.ListBucketsResponse
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.Tag
import com.google.common.truth.Truth.assertThat
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {
    private val awsClientFactory: AwsClientFactory = mockk()
    private val secureStorage: SecureStorageDataSource = mockk(relaxed = true)

    // We will initialize this in each test using runTest's scope or mock logic
    private lateinit var repository: AuthRepositoryImpl

    private val bootstrapCreds =
        BootstrapCredentials(
            accessKeyId = "ASIA...",
            secretAccessKey = "secret",
            sessionToken = "token",
            region = "us-east-1",
        )

    private val runtimeCreds =
        RuntimeCredentials(
            accessKeyId = "AKIA...",
            secretAccessKey = "secret",
            bucketName = "locus-bucket",
            region = "us-east-1",
            accountId = "123456789012",
        )

    @Before
    fun setup() {
        // Default storage behavior: empty
        coEvery { secureStorage.getRuntimeCredentials() } returns LocusResult.Success(null)
        coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(null)
    }

    @Test
    fun `init loads Authenticated state when Runtime credentials exist`() =
        runTest {
            coEvery { secureStorage.getRuntimeCredentials() } returns LocusResult.Success(runtimeCreds)

            // Pass 'this' (TestScope) so advanceUntilIdle() works reliably on the launch block
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)

            // Wait for init block to complete
            advanceUntilIdle()

            repository.getAuthState().test {
                assertThat(awaitItem()).isEqualTo(AuthState.Authenticated)
            }
        }

    @Test
    fun `init loads SetupPending state when Bootstrap credentials exist`() =
        runTest {
            coEvery { secureStorage.getRuntimeCredentials() } returns LocusResult.Success(null)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(bootstrapCreds)

            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)

            advanceUntilIdle()

            repository.getAuthState().test {
                assertThat(awaitItem()).isEqualTo(AuthState.SetupPending)
            }
        }

    @Test
    fun `init loads Uninitialized state when no credentials exist`() =
        runTest {
            coEvery { secureStorage.getRuntimeCredentials() } returns LocusResult.Success(null)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(null)

            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)

            advanceUntilIdle()

            repository.getAuthState().test {
                assertThat(awaitItem()).isEqualTo(AuthState.Uninitialized)
            }
        }

    @Test
    fun `promoteToRuntimeCredentials saves runtime, deletes bootstrap, and updates state`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)

            coEvery { secureStorage.saveRuntimeCredentials(any()) } returns LocusResult.Success(Unit)
            coEvery { secureStorage.clearBootstrapCredentials() } returns LocusResult.Success(Unit)

            repository.getAuthState().test {
                // Initial state
                assertThat(awaitItem()).isEqualTo(AuthState.Uninitialized)

                val result = repository.promoteToRuntimeCredentials(runtimeCreds)

                assertThat(result).isInstanceOf(LocusResult.Success::class.java)
                assertThat(awaitItem()).isEqualTo(AuthState.Authenticated)
            }

            repository.getProvisioningState().test {
                assertThat(awaitItem()).isEqualTo(ProvisioningState.Success)
            }

            coVerify { secureStorage.saveRuntimeCredentials(runtimeCreds) }
            coVerify { secureStorage.clearBootstrapCredentials() }
        }

    @Test
    fun `validateBucket returns Available when bucket exists and has correct tag`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(bootstrapCreds)

            val s3Client = mockk<S3Client>(relaxed = true)
            every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client

            coEvery { s3Client.headBucket(any<HeadBucketRequest>()) } returns mockk() // Success implies exists
            coEvery { s3Client.getBucketTagging(any<GetBucketTaggingRequest>()) } returns
                GetBucketTaggingResponse {
                    tagSet =
                        listOf(
                            Tag {
                                key = "LocusRole"
                                value = "DeviceBucket"
                            },
                        )
                }

            val result = repository.validateBucket("locus-bucket")

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            assertThat((result as LocusResult.Success).data).isEqualTo(BucketValidationStatus.Available)

            coVerify { s3Client.headBucket(any<HeadBucketRequest>()) }
            coVerify { s3Client.getBucketTagging(any<GetBucketTaggingRequest>()) }
        }

    @Test
    fun `validateBucket returns Invalid when tag is missing`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(bootstrapCreds)

            val s3Client = mockk<S3Client>(relaxed = true)
            every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client

            coEvery { s3Client.headBucket(any<HeadBucketRequest>()) } returns mockk()
            coEvery { s3Client.getBucketTagging(any<GetBucketTaggingRequest>()) } returns
                GetBucketTaggingResponse {
                    tagSet =
                        listOf(
                            Tag {
                                key = "OtherRole"
                                value = "SomethingElse"
                            },
                        )
                }

            val result = repository.validateBucket("locus-bucket")

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val status = (result as LocusResult.Success).data
            assertThat(status).isInstanceOf(BucketValidationStatus.Invalid::class.java)
            assertThat((status as BucketValidationStatus.Invalid).reason).contains("missing required LocusRole tag")
        }

    @Test
    fun `validateBucket returns Failure when network error occurs`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(bootstrapCreds)

            val s3Client = mockk<S3Client>(relaxed = true)
            every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client

            coEvery { s3Client.headBucket(any<HeadBucketRequest>()) } throws RuntimeException("Network Error")

            val result = repository.validateBucket("locus-bucket")

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.NetworkError.Generic::class.java)
        }

    @Test
    fun `validateBucket returns Invalid when bucket does not exist`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(bootstrapCreds)

            val s3Client = mockk<S3Client>(relaxed = true)
            every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client

            // Mocking NotFound exception for headBucket
            coEvery { s3Client.headBucket(any<HeadBucketRequest>()) } throws mockk<NotFound>(relaxed = true)

            val result = repository.validateBucket("locus-bucket")

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val status = (result as LocusResult.Success).data
            assertThat(status).isInstanceOf(BucketValidationStatus.Invalid::class.java)
            assertThat((status as BucketValidationStatus.Invalid).reason).contains("not found")
        }

    @Test
    fun `scanForRecoveryBuckets filters correctly`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(bootstrapCreds)

            val s3Client = mockk<S3Client>(relaxed = true)
            every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client

            coEvery { s3Client.listBuckets() } returns
                ListBucketsResponse {
                    buckets =
                        listOf(
                            aws.sdk.kotlin.services.s3.model.Bucket { name = "locus-bucket-1" },
                            aws.sdk.kotlin.services.s3.model.Bucket { name = "other-bucket" },
                            aws.sdk.kotlin.services.s3.model.Bucket { name = "locus-bucket-2" },
                        )
                }

            val result = repository.scanForRecoveryBuckets()

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val buckets = (result as LocusResult.Success).data
            assertThat(buckets).containsExactly("locus-bucket-1", "locus-bucket-2")
        }

    @Test
    fun `recoverAccount parses stack outputs correctly`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            coEvery { secureStorage.getBootstrapCredentials() } returns LocusResult.Success(bootstrapCreds)

            val s3Client = mockk<S3Client>(relaxed = true)
            every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client
            coEvery { s3Client.getBucketTagging(any<GetBucketTaggingRequest>()) } returns
                GetBucketTaggingResponse {
                    tagSet =
                        listOf(
                            Tag {
                                key = "aws:cloudformation:stack-name"
                                value = "locus-stack"
                            },
                        )
                }

            val cfClient = mockk<CloudFormationClient>(relaxed = true)
            every { awsClientFactory.createBootstrapCloudFormationClient(any()) } returns cfClient

            coEvery { cfClient.describeStacks(any<DescribeStacksRequest>()) } returns
                DescribeStacksResponse {
                    stacks =
                        listOf(
                            Stack {
                                stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-stack/uuid"
                                outputs =
                                    listOf(
                                        Output {
                                            outputKey = "RuntimeAccessKeyId"
                                            outputValue = "AKIA_RUNTIME"
                                        },
                                        Output {
                                            outputKey = "RuntimeSecretAccessKey"
                                            outputValue = "SECRET_RUNTIME"
                                        },
                                        Output {
                                            outputKey = "BucketName"
                                            outputValue = "locus-bucket-recovered"
                                        },
                                    )
                            },
                        )
                }

            val result = repository.recoverAccount("locus-bucket-recovered", "new-device")

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val creds = (result as LocusResult.Success).data
            assertThat(creds.accessKeyId).isEqualTo("AKIA_RUNTIME")
            assertThat(creds.secretAccessKey).isEqualTo("SECRET_RUNTIME")
            assertThat(creds.bucketName).isEqualTo("locus-bucket-recovered")
            assertThat(creds.accountId).isEqualTo("123456789012")
        }
}
