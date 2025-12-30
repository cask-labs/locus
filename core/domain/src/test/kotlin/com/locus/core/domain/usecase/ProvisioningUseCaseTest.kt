package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.StackDetails
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.RuntimeCredentials
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
    private val cloudFormationClient = mockk<CloudFormationClient>()
    private val resourceProvider = mockk<ResourceProvider>()

    private val useCase =
        ProvisioningUseCase(
            authRepository,
            configRepository,
            cloudFormationClient,
            resourceProvider,
        )

    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
    private val deviceName = "my-device"
    private val template = "template-body"
    private val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-my-device/uuid"

    @Test
    fun `returns failure when device name is blank`() =
        runBlocking {
            val result = useCase(creds, "")
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(DomainException.AuthError.InvalidCredentials)
        }

    @Test
    fun `returns failure when template loading fails`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } throws RuntimeException("File not found")

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.NetworkError.Generic::class.java)
        }

    @Test
    fun `returns failure when stack creation fails`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            val expectedError = DomainException.NetworkError.Generic(Exception("AWS Error"))
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
            coVerify { authRepository.updateProvisioningState(match { it is com.locus.core.domain.model.auth.ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when stack creation fails with unknown error`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            val originalError = Exception("Unknown")
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Failure(originalError)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isEqualTo(originalError)
        }

    @Test
    fun `returns failure when stack reaches failed state`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails(stackId, "CREATE_FAILED", null))

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
        }

    @Test
    fun `returns failure when stack completes but outputs are missing`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails(stackId, "CREATE_COMPLETE", null))

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Missing stack outputs")
        }

    @Test
    fun `returns failure when stack completes but outputs are invalid`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails(stackId, "CREATE_COMPLETE", mapOf("SomeKey" to "SomeValue")))

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `returns failure when secret key is missing`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        stackId,
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "BucketName" to "rb",
                        ),
                    ),
                )

            val result = useCase(creds, deviceName)
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `returns failure when bucket name is missing`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        stackId,
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                        ),
                    ),
                )

            val result = useCase(creds, deviceName)
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `returns failure when account ID is invalid`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            val invalidStackId = "invalid-arn"
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        invalidStackId,
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )

            val result = useCase(creds, deviceName)
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `returns failure when identity initialization fails`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        stackId,
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )

            val expectedError = DomainException.AuthError.Generic(Exception("Init failed"))
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }

    @Test
    fun `returns failure when credential promotion fails`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        stackId,
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)

            val expectedError = DomainException.AuthError.Generic(Exception("Promote failed"))
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }

    @Test
    fun `successful provisioning flow with retries`() =
        runBlocking {
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returnsMany
                listOf(
                    // Loop continues
                    LocusResult.Failure(Exception("Network Glitch")),
                    // Loop continues
                    LocusResult.Success(StackDetails(stackId, "CREATE_IN_PROGRESS", null)),
                    LocusResult.Success(
                        StackDetails(
                            stackId,
                            "CREATE_COMPLETE",
                            mapOf(
                                "RuntimeAccessKeyId" to "rk",
                                "RuntimeSecretAccessKey" to "rs",
                                "BucketName" to "rb",
                            ),
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            coVerify(exactly = 3) { cloudFormationClient.describeStack(creds, any()) }
        }

    @Test
    fun `successful provisioning flow`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            // Mock polling: first In Progress, then Complete
            coEvery { cloudFormationClient.describeStack(any(), any()) } returnsMany
                listOf(
                    LocusResult.Success(StackDetails(stackId, "CREATE_IN_PROGRESS", null)),
                    LocusResult.Success(
                        StackDetails(
                            stackId,
                            "CREATE_COMPLETE",
                            mapOf(
                                "RuntimeAccessKeyId" to "rk",
                                "RuntimeSecretAccessKey" to "rs",
                                "BucketName" to "rb",
                            ),
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)

            coVerify {
                cloudFormationClient.createStack(
                    creds,
                    "locus-user-$deviceName",
                    template,
                    mapOf("StackName" to deviceName),
                )
            }
            coVerify(atLeast = 2) { cloudFormationClient.describeStack(creds, "locus-user-$deviceName") }
            coVerify { configRepository.initializeIdentity(any(), any()) }

            val slot = slot<RuntimeCredentials>()
            coVerify { authRepository.promoteToRuntimeCredentials(capture(slot)) }
            assertThat(slot.captured.accessKeyId).isEqualTo("rk")
            assertThat(slot.captured.bucketName).isEqualTo("rb")
            assertThat(slot.captured.accountId).isEqualTo("123456789012")
        }
}
