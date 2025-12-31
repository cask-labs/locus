package com.locus.core.domain.infrastructure

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.InfrastructureConstants.STATUS_CREATE_COMPLETE
import com.locus.core.domain.infrastructure.InfrastructureConstants.STATUS_CREATE_FAILED
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
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
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val timeProvider = mockk<TimeProvider>(relaxed = true)

    private val service = StackProvisioningService(
        cloudFormationClient,
        authRepository,
        timeProvider
    )

    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
    private val stackName = "test-stack"
    private val template = "template-body"
    private val params = mapOf("Key" to "Value")
    private val stackId = "stack-id"

    @Test
    fun `returns failure when stack creation fails`() = runBlocking {
        val error = DomainException.NetworkError.Generic(Exception("Network"))
        coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Failure(error)

        val result = service.createAndPollStack(creds, stackName, template, params)

        assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
        assertThat((result as LocusResult.Failure).error).isEqualTo(error)
        coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
    }

    @Test
    fun `polls successfully until complete`() = runBlocking {
        coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

        coEvery { cloudFormationClient.describeStack(creds, stackName) } returnsMany listOf(
            LocusResult.Success(StackDetails(stackId, "CREATE_IN_PROGRESS", null)),
            LocusResult.Success(StackDetails(stackId, STATUS_CREATE_COMPLETE, mapOf("Out" to "Val")))
        )

        val result = service.createAndPollStack(creds, stackName, template, params)

        assertThat(result).isInstanceOf(LocusResult.Success::class.java)
        val data = (result as LocusResult.Success).data
        assertThat(data.stackId).isEqualTo(stackId)
        assertThat(data.outputs).containsEntry("Out", "Val")
        coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.WaitingForCompletion }) }
    }

    @Test
    fun `fails fast on permanent error`() = runBlocking {
        coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

        coEvery { cloudFormationClient.describeStack(creds, stackName) } returns
            LocusResult.Success(StackDetails(stackId, STATUS_CREATE_FAILED, null))

        val result = service.createAndPollStack(creds, stackName, template, params)

        assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
        val error = (result as LocusResult.Failure).error
        assertThat(error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
    }

    @Test
    fun `retries on transient error`() = runBlocking {
        coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

        coEvery { cloudFormationClient.describeStack(creds, stackName) } returnsMany listOf(
            LocusResult.Failure(DomainException.NetworkError.Timeout()), // Transient
            LocusResult.Success(StackDetails(stackId, STATUS_CREATE_COMPLETE, mapOf("Out" to "Val")))
        )

        val result = service.createAndPollStack(creds, stackName, template, params)

        assertThat(result).isInstanceOf(LocusResult.Success::class.java)
    }

    @Test
    fun `fails fast on permanent polling error`() = runBlocking {
        coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)

        // AuthError is considered permanent
        val permError = DomainException.AuthError.AccessDenied
        coEvery { cloudFormationClient.describeStack(creds, stackName) } returns LocusResult.Failure(permError)

        val result = service.createAndPollStack(creds, stackName, template, params)

        assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
        assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
    }

    @Test
    fun `times out when polling exceeds limit`() = runBlocking {
        coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success(stackId)
        coEvery { cloudFormationClient.describeStack(creds, stackName) } returns
             LocusResult.Success(StackDetails(stackId, "CREATE_IN_PROGRESS", null))

        every { timeProvider.currentTimeMillis() } returnsMany listOf(0L, 700_000L) // Start, then timeout

        val result = service.createAndPollStack(creds, stackName, template, params)

        assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
        assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.NetworkError.Timeout::class.java)
    }
}
