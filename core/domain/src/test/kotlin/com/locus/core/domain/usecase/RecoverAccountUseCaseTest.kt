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
            val error = DomainException.NetworkError.Offline
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Failure(error)

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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(emptyMap())

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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(mapOf(TAG_STACK_NAME to "stack-name"))
            every { resourceProvider.getStackTemplate() } throws RuntimeException("File not found")

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.InvalidConfiguration::class.java)
        }

    @Test
    fun `returns failure when stack creation fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(mapOf(TAG_STACK_NAME to "stack-name"))
            every { resourceProvider.getStackTemplate() } returns template

            val originalError = DomainException.NetworkError.Generic(Exception("Network"))
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns LocusResult.Failure(originalError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isEqualTo(originalError)
        }

    @Test
    fun `returns failure when stack completes but outputs are missing`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(mapOf(TAG_STACK_NAME to "stack-name"))
            every { resourceProvider.getStackTemplate() } returns template

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(StackProvisioningResult(stackId, emptyMap()))

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Invalid stack outputs")
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when identity initialization fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(mapOf(TAG_STACK_NAME to "stack-name"))
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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(mapOf(TAG_STACK_NAME to "stack-name"))
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
            val domainError = DomainException.AuthError.AccessDenied
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Failure(domainError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(domainError)
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `successful recovery flow`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(mapOf(TAG_STACK_NAME to "stack-name"))
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

            // Verify
            coVerify {
                stackProvisioningService.createAndPollStack(
                    creds,
                    match { it.startsWith("locus-user-") },
                    template,
                    match { it["BucketName"] == bucketName },
                )
            }

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
