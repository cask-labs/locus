package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.S3Client
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

class RecoverAccountUseCaseTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val configRepository = mockk<ConfigurationRepository>(relaxed = true)
    private val cloudFormationClient = mockk<CloudFormationClient>()
    private val s3Client = mockk<S3Client>()
    private val resourceProvider = mockk<ResourceProvider>()

    private val useCase =
        RecoverAccountUseCase(
            authRepository,
            configRepository,
            cloudFormationClient,
            s3Client,
            resourceProvider,
        )

    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
    private val bucketName = "locus-bucket-old"
    private val template = "template-body"
    private val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-uuid/uuid"

    @Test
    fun `returns failure when bucket tags fetch fails`() =
        runBlocking {
            val expectedError = DomainException.NetworkError.Generic(Exception("Network Error"))
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(DomainException.RecoveryError.MissingStackTag)
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `returns failure when bucket tags missing stack name`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns LocusResult.Success(emptyMap())

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(DomainException.RecoveryError.MissingStackTag)
        }

    @Test
    fun `returns failure when template loading fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } throws RuntimeException("File not found")

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.NetworkError.Generic::class.java)
        }

    @Test
    fun `returns failure when stack creation fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit

            val expectedError = DomainException.NetworkError.Generic(Exception("AWS Error"))
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }

    @Test
    fun `returns failure when stack creation fails with unknown error`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit

            val originalError = Exception("Unknown")
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Failure(originalError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isEqualTo(originalError)
        }

    @Test
    fun `returns failure when stack reaches failed state`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails(stackId, "CREATE_FAILED", null))

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
        }

    @Test
    fun `returns failure when stack completes but outputs are missing`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails(stackId, "CREATE_COMPLETE", null))

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Missing stack outputs")
        }

    @Test
    fun `returns failure when stack completes but outputs are invalid`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails(stackId, "CREATE_COMPLETE", mapOf("SomeKey" to "SomeValue")))

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `returns failure when secret key is missing`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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
                        ),
                    ),
                )

            val result = useCase(creds, bucketName)
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `returns failure when account ID is invalid`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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
                        ),
                    ),
                )

            val result = useCase(creds, bucketName)
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `returns failure when identity initialization fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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

            val expectedError = DomainException.AuthError.Generic(Exception("Init failed"))
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }

    @Test
    fun `returns failure when credential promotion fails`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)

            val expectedError = DomainException.AuthError.Generic(Exception("Promote failed"))
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Failure(expectedError)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(expectedError)
        }

    @Test
    fun `successful recovery flow with retries`() =
        runBlocking {
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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
                            ),
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            val result = useCase(creds, bucketName)

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            coVerify(exactly = 3) { cloudFormationClient.describeStack(creds, any()) }
        }

    @Test
    fun `successful recovery flow`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))

            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

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
                            ),
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)

            // Verify we created a new stack (captured stack name will vary due to UUID)
            coVerify {
                cloudFormationClient.createStack(
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
        }
}
