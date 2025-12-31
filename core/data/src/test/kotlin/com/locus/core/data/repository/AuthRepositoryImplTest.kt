package com.locus.core.data.repository

import app.cash.turbine.test
import aws.sdk.kotlin.services.s3.S3Client
import com.google.common.truth.Truth.assertThat
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
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

            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            repository.initialize()

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
            repository.initialize()

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
            repository.initialize()

            advanceUntilIdle()

            repository.getAuthState().test {
                assertThat(awaitItem()).isEqualTo(AuthState.Uninitialized)
            }
        }

    @Test
    fun `promoteToRuntimeCredentials saves runtime, deletes bootstrap, and updates state`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            repository.initialize()

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
    fun `validateCredentials returns Success when listBuckets succeeds`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            val s3Client = mockk<S3Client>(relaxed = true)
            io.mockk.every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client
            coEvery { s3Client.listBuckets() } returns mockk()

            val result = repository.validateCredentials(bootstrapCreds)

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            coVerify { s3Client.listBuckets() }
        }

    @Test
    fun `validateCredentials returns Failure when listBuckets fails`() =
        runTest {
            repository = AuthRepositoryImpl(awsClientFactory, secureStorage, this)
            val s3Client = mockk<S3Client>(relaxed = true)
            io.mockk.every { awsClientFactory.createBootstrapS3Client(any()) } returns s3Client
            coEvery { s3Client.listBuckets() } throws RuntimeException("Network error")

            val result = repository.validateCredentials(bootstrapCreds)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.AuthError.InvalidCredentials::class.java)
        }
}
