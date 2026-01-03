package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.InfrastructureConstants.TAG_STACK_NAME
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.S3Client
import com.locus.core.domain.infrastructure.StackProvisioningService
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.model.auth.StackProvisioningResult
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RecoverAccountUseCaseTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val configRepository = mockk<ConfigurationRepository>(relaxed = true)
    private val s3Client = mockk<S3Client>()
    private val resourceProvider = mockk<ResourceProvider>()
    private val stackProvisioningService = mockk<StackProvisioningService>()

    private val useCase =
        RecoverAccountUseCase(
            authRepository,
            configRepository,
            s3Client,
            resourceProvider,
            stackProvisioningService,
        )

    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
    private val bucketName = "locus-user-123"
    private val template = "template-body"
    private val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-uuid/uuid"

    @Test
    fun `returns failure when bucket tag retrieval fails`() =
        runBlocking {
            val expectedError = DomainException.NetworkError.Offline
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(DomainException.RecoveryError.MissingStackTag)
            coVerify {
                authRepository.updateProvisioningState(
                    match { it is ProvisioningState.Working && it.currentStep == "Validating bucket..." },
                )
            }
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when stack name tag is missing`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("SomeTag" to "Value"))

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(DomainException.RecoveryError.MissingStackTag)
            coVerify {
                authRepository.updateProvisioningState(
                    match { it is ProvisioningState.Working && it.currentStep == "Validating bucket..." },
                )
            }
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when template loading fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf(TAG_STACK_NAME to "device-1"))
            every { resourceProvider.getStackTemplate() } throws RuntimeException("Template missing")

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat(
                (result as LocusResult.Failure).error,
            ).isInstanceOf(DomainException.ProvisioningError.InvalidConfiguration::class.java)
        }

    @Test
    fun `returns failure when stack provisioning fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf(TAG_STACK_NAME to "device-1"))
            every { resourceProvider.getStackTemplate() } returns template

            val expectedError = DomainException.ProvisioningError.DeploymentFailed("Failed")
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }

    @Test
    fun `returns failure when outputs are missing`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf(TAG_STACK_NAME to "device-1"))
            every { resourceProvider.getStackTemplate() } returns template

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(StackProvisioningResult(stackId, emptyMap()))

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when identity init fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf(TAG_STACK_NAME to "device-1"))
            every { resourceProvider.getStackTemplate() } returns template

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf("RuntimeAccessKeyId" to "k", "RuntimeSecretAccessKey" to "s"),
                    ),
                )

            val expectedError = DomainException.AuthError.Generic(Exception("Init failed"))
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
            coVerify {
                authRepository.updateProvisioningState(
                    match { it is ProvisioningState.Working && it.currentStep == "Finalizing setup..." },
                )
            }
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when promotion fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf(TAG_STACK_NAME to "device-1"))
            every { resourceProvider.getStackTemplate() } returns template

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf("RuntimeAccessKeyId" to "k", "RuntimeSecretAccessKey" to "s"),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)

            val expectedError = DomainException.AuthError.AccessDenied
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `successful recovery flow`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf(TAG_STACK_NAME to "device-1"))
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)

            val slot = slot<RuntimeCredentials>()
            coVerify { authRepository.promoteToRuntimeCredentials(capture(slot)) }
            assertThat(slot.captured.accessKeyId).isEqualTo("rk")
            assertThat(slot.captured.bucketName).isEqualTo(bucketName)
            assertThat(slot.captured.accountId).isEqualTo("123456789012")

            coVerify {
                authRepository.updateProvisioningState(
                    match { it is ProvisioningState.Working && it.currentStep == "Finalizing setup..." },
                )
            }
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Success }) }
        }
}
