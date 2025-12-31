package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.ResourceProvider
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

class ProvisioningUseCaseTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val configRepository = mockk<ConfigurationRepository>(relaxed = true)
    private val resourceProvider = mockk<ResourceProvider>()
    private val stackProvisioningService = mockk<StackProvisioningService>()

    private val useCase =
        ProvisioningUseCase(
            authRepository,
            configRepository,
            resourceProvider,
            stackProvisioningService,
        )

    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
    private val deviceName = "test-device"
    private val template = "template-body"
    private val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-uuid/uuid"

    @Test
    fun `returns failure when device name is blank`() =
        runBlocking {
            val result = useCase(creds, "")

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(DomainException.AuthError.InvalidCredentials)
            // Code does NOT call updateProvisioningState for this case
            coVerify(exactly = 0) { authRepository.updateProvisioningState(any()) }
        }

    @Test
    fun `returns failure when template loading fails`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } throws RuntimeException("File not found")

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.InvalidConfiguration::class.java)
        }

    @Test
    fun `returns failure when stack creation fails`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            // StackProvisioningService handles its own state updates, so we just check failure propagation

            val originalError = DomainException.NetworkError.Generic(Exception("Network"))
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns LocusResult.Failure(originalError)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isEqualTo(originalError)
        }

    @Test
    fun `returns failure when stack completes but outputs are missing`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template

            // This case should be handled by StackProvisioningService, but if it returns outputs as empty?
            // The service returns StackProvisioningResult which has map.
            // If the map is missing required keys, UseCase handles it.

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(StackProvisioningResult(stackId, emptyMap()))

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Invalid stack outputs")
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when identity initialization fails`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "bucket",
                        ),
                    ),
                )

            val expectedError = DomainException.AuthError.Generic(Exception("Init failed"))
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.FinalizingSetup }) }
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `successful provisioning flow`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit

            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "bucket",
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)

            // Verify
            coVerify {
                stackProvisioningService.createAndPollStack(
                    creds,
                    match { it.startsWith("locus-user-") },
                    template,
                    match { it["StackName"] == deviceName },
                )
            }

            val slot = slot<RuntimeCredentials>()
            coVerify { authRepository.promoteToRuntimeCredentials(capture(slot)) }
            assertThat(slot.captured.accessKeyId).isEqualTo("rk")
            assertThat(slot.captured.bucketName).isEqualTo("bucket")
            assertThat(slot.captured.accountId).isEqualTo("123456789012")
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.FinalizingSetup }) }
        }
}
