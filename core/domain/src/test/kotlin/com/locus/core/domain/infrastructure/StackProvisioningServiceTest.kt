package com.locus.core.domain.infrastructure

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.InfrastructureConstants.STATUS_CREATE_COMPLETE
import com.locus.core.domain.infrastructure.InfrastructureConstants.STATUS_CREATE_FAILED
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.util.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class StackProvisioningServiceTest {
    private val cloudFormationClient = mockk<CloudFormationClient>()
    private val timeProvider = mockk<TimeProvider>(relaxed = true)

    private val service =
        StackProvisioningService(
            cloudFormationClient,
            timeProvider,
        )

    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
    private val stackName = "test-stack"
    private val template = "template-body"
    private val params = mapOf("Key" to "Value")
    private val stackId = "stack-id"
    private val onStatusUpdate: suspend (String) -> Unit = mockk(relaxed = true)

    @Test
    fun `returns failure when stack creation fails`() =
        runBlocking {
            val error = DomainException.NetworkError.Generic(Exception("Network"))
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Failure(error)

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(error)
            // No state update should happen via callback if creation fails immediately
            coVerify(exactly = 1) { onStatusUpdate(any()) } // "Deploying..." call
        }

    @Test
    fun `returns failure when stack creation fails with non-domain exception`() =
        runBlocking {
            val error = RuntimeException("Unexpected")
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Failure(error)

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val failure = result as LocusResult.Failure
            assertThat(failure.error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(failure.error.message).contains("Unexpected")
        }

    @Test
    fun `polls successfully until complete`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

            coEvery { cloudFormationClient.describeStack(creds, stackName) } returnsMany
                listOf(
                    LocusResult.Success(StackDetails(stackId, "CREATE_IN_PROGRESS", null)),
                    LocusResult.Success(StackDetails(stackId, STATUS_CREATE_COMPLETE, mapOf("Out" to "Val"))),
                )

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val data = (result as LocusResult.Success).data
            assertThat(data.stackId).isEqualTo(stackId)
            assertThat(data.outputs).containsEntry("Out", "Val")
            coVerify { onStatusUpdate(match { it.startsWith("Status:") }) }
        }

    @Test
    fun `fails fast on permanent error`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

            coEvery { cloudFormationClient.describeStack(creds, stackName) } returns
                LocusResult.Success(StackDetails(stackId, STATUS_CREATE_FAILED, null))

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
        }

    @Test
    fun `retries on transient error`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

            coEvery { cloudFormationClient.describeStack(creds, stackName) } returnsMany
                listOf(
                    // Transient
                    LocusResult.Failure(DomainException.NetworkError.Timeout()),
                    LocusResult.Success(StackDetails(stackId, STATUS_CREATE_COMPLETE, mapOf("Out" to "Val"))),
                )

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
        }

    @Test
    fun `fails fast on permanent polling error`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

            // AuthError is considered permanent
            val permError = DomainException.AuthError.AccessDenied
            coEvery { cloudFormationClient.describeStack(creds, stackName) } returns LocusResult.Failure(permError)

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
        }

    @Test
    fun `fails fast on quota error`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

            val quotaError = DomainException.ProvisioningError.Quota("Quota exceeded")
            coEvery { cloudFormationClient.describeStack(creds, stackName) } returns LocusResult.Failure(quotaError)

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
        }

    @Test
    fun `fails fast on permissions error`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

            val permError = DomainException.ProvisioningError.Permissions("Permissions denied")
            coEvery { cloudFormationClient.describeStack(creds, stackName) } returns LocusResult.Failure(permError)

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
        }

    @Test
    fun `returns failure when outputs are missing on complete`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

            coEvery { cloudFormationClient.describeStack(creds, stackName) } returns
                LocusResult.Success(StackDetails(stackId, STATUS_CREATE_COMPLETE, null))

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            val error = (result as LocusResult.Failure).error
            assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            assertThat(error.message).contains("Missing stack outputs")
        }

    @Test
    fun `times out when polling exceeds limit`() =
        runBlocking {
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)
            coEvery { cloudFormationClient.describeStack(creds, stackName) } returns
                LocusResult.Success(StackDetails(stackId, "CREATE_IN_PROGRESS", null))

            // Start, then timeout
            every { timeProvider.currentTimeMillis() } returnsMany listOf(0L, 700_000L)

            val result = service.createAndPollStack(creds, stackName, template, params, onStatusUpdate)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.NetworkError.Timeout::class.java)
        }
}
