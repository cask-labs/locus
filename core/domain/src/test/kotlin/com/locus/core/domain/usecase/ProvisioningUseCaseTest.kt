package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.StackDetails
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
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
    private val stackName = "locus-user-my-device"

    @Test
    fun `successful provisioning flow`() =
        runBlocking {
            // Given
            val template = "template-body"
            val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-my-device/uuid"

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
                    stackName,
                    template,
                    mapOf("StackName" to deviceName),
                )
            }
            coVerify(atLeast = 2) { cloudFormationClient.describeStack(creds, stackName) }
            coVerify { configRepository.initializeIdentity(any(), any()) }

            val slot = slot<RuntimeCredentials>()
            coVerify { authRepository.promoteToRuntimeCredentials(capture(slot)) }
            assertThat(slot.captured.accessKeyId).isEqualTo("rk")
            assertThat(slot.captured.bucketName).isEqualTo("rb")
            assertThat(slot.captured.accountId).isEqualTo("123456789012")
        }

    @Test
    fun `fails when device name is blank`() =
        runBlocking {
            // When
            val result = useCase(creds, "")

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.AuthError.InvalidCredentials::class.java)
        }

    @Test
    fun `fails when template load fails`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } throws RuntimeException("File error")

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.NetworkError.Generic::class.java)
        }

    @Test
    fun `fails when stack creation returns failure`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            val failure = LocusResult.Failure(DomainException.NetworkError.Generic(RuntimeException("AWS error")))
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns failure

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isEqualTo(failure)
            coVerify { authRepository.updateProvisioningState(any<ProvisioningState.Failure>()) }
        }

    @Test
    fun `fails when stack creation fails remotely`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns LocusResult.Success(StackDetails("id", "CREATE_FAILED", null))

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Stack creation failed: CREATE_FAILED")
        }

    @Test
    fun `fails when stack creation rolls back`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns LocusResult.Success(StackDetails("id", "ROLLBACK_COMPLETE", null))

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Stack creation failed: ROLLBACK_COMPLETE")
        }

    @Test
    fun `fails when stack creation rolls back in progress`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns LocusResult.Success(StackDetails("id", "ROLLBACK_IN_PROGRESS", null))

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Stack creation failed: ROLLBACK_IN_PROGRESS")
        }

    @Test
    fun `fails when stack creation returns failure with no message`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            val failure = LocusResult.Failure(RuntimeException()) // No message
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns failure

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isEqualTo(failure)
            val error = (result as LocusResult.Failure).error
            // Since UseCase re-wraps it if it catches it, but here it returns failure directly?
            // Wait, check implementation:
            /*
            if (createResult is LocusResult.Failure) {
                val error = createResult.error as? DomainException
                        ?: DomainException.ProvisioningError.DeploymentFailed(
                            createResult.error.message ?: "Unknown error",
                        )
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return createResult as LocusResult.Failure
            }
            */
            // It RETURNS createResult AS IS. It uses the constructed error ONLY for updateProvisioningState.
            // So assert on the captured state.
            val slots = mutableListOf<ProvisioningState>()
            coVerify { authRepository.updateProvisioningState(capture(slots)) }
            val state = slots.last()
            assertThat(state).isInstanceOf(ProvisioningState.Failure::class.java)
            val stateError = (state as ProvisioningState.Failure).error
            assertThat(stateError.message).isEqualTo("Unknown error")
        }

    @Test
    fun `fails when stack id is null`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        null,
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when stack outputs are missing`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails("id", "CREATE_COMPLETE", null)) // No outputs

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Missing stack outputs")
        }

    @Test
    fun `fails when stack outputs are incomplete`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "id",
                        "CREATE_COMPLETE",
                        mapOf("RuntimeAccessKeyId" to "key"), // Missing secret and bucket
                    ),
                )

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when stack outputs missing access key`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "arn:aws:cloudformation:::stack/x/y",
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when stack outputs missing bucket`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "arn:aws:cloudformation:::stack/x/y",
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                        ),
                    ),
                )

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when stack id is malformed (missing account id)`() =
        runBlocking {
            // Given
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "malformed-stack-id", // Not an ARN
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when identity initialization fails`() =
        runBlocking {
            // Given
            val ex = Exception("Disk error")
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "arn:aws:cloudformation:::stack/x/y",
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Failure(ex)

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            // It returns the repo failure directly.
            assertThat(error).isInstanceOf(Exception::class.java)
            assertThat(error.message).isEqualTo("Disk error")
        }

    @Test
    fun `fails when credential promotion fails`() =
        runBlocking {
            // Given
            val ex = Exception("Store error")
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "arn:aws:cloudformation:::stack/x/y",
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                            "BucketName" to "rb",
                        ),
                    ),
                )
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Failure(ex)

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            // Same here, it returns the repo failure directly.
            assertThat(error).isInstanceOf(Exception::class.java)
            assertThat(error.message).isEqualTo("Store error")
        }
}
