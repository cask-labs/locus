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

    @Test
    fun `successful recovery flow`() =
        runBlocking {
            // Given
            val template = "template-body"
            val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-uuid/uuid"

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

    @Test
    fun `fails when getting bucket tags fails`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Failure(DomainException.RecoveryError.MissingStackTag)

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.RecoveryError.MissingStackTag::class.java)
        }

    @Test
    fun `fails when bucket tags are missing required tag`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("OtherTag" to "value"))

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.RecoveryError.MissingStackTag::class.java)
        }

    @Test
    fun `fails when template load fails`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } throws RuntimeException("File error")

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.NetworkError.Generic::class.java)
        }

    @Test
    fun `fails when stack creation returns failure`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            val failure = LocusResult.Failure(DomainException.NetworkError.Generic(RuntimeException("AWS error")))
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns failure

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isEqualTo(failure)
            coVerify { authRepository.updateProvisioningState(any<ProvisioningState.Failure>()) }
        }

    @Test
    fun `fails when stack creation fails remotely`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns LocusResult.Success(StackDetails("id", "CREATE_FAILED", null))

            // When
            val result = useCase(creds, bucketName)

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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns LocusResult.Success(StackDetails("id", "ROLLBACK_COMPLETE", null))

            // When
            val result = useCase(creds, bucketName)

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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns LocusResult.Success(StackDetails("id", "ROLLBACK_IN_PROGRESS", null))

            // When
            val result = useCase(creds, bucketName)

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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            val failure = LocusResult.Failure(RuntimeException()) // No message
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns failure

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isEqualTo(failure)
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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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
                        ),
                    ),
                )

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when stack outputs are missing`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(StackDetails("id", "CREATE_COMPLETE", null)) // No outputs

            // When
            val result = useCase(creds, bucketName)

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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "id",
                        "CREATE_COMPLETE",
                        mapOf("RuntimeAccessKeyId" to "key"), // Missing secret
                    ),
                )

            // When
            val result = useCase(creds, bucketName)

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
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "arn:aws:cloudformation:::stack/x/y",
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeSecretAccessKey" to "rs",
                        ),
                    ),
                )

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when stack id is malformed (missing account id)`() =
        runBlocking {
            // Given
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
            every { resourceProvider.getStackTemplate() } returns "template"
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")
            coEvery { cloudFormationClient.describeStack(any(), any()) } returns
                LocusResult.Success(
                    StackDetails(
                        "malformed",
                        "CREATE_COMPLETE",
                        mapOf(
                            "RuntimeAccessKeyId" to "rk",
                            "RuntimeSecretAccessKey" to "rs",
                        ),
                    ),
                )

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Invalid stack outputs")
        }

    @Test
    fun `fails when identity initialization fails`() =
        runBlocking {
            // Given
            val ex = Exception("Disk error")
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Failure(ex)

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(Exception::class.java)
            assertThat(error.message).isEqualTo("Disk error")
        }

    @Test
    fun `fails when credential promotion fails`() =
        runBlocking {
            // Given
            val ex = Exception("Store error")
            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))
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
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Failure(ex)

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(Exception::class.java)
            assertThat(error.message).isEqualTo("Store error")
        }
}
