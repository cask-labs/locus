package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.StackDetails
import com.locus.core.domain.infrastructure.StackProvisioningService
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.StackProvisioningResult
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProvisioningUseCaseTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val configRepository = mockk<ConfigurationRepository>(relaxed = true)
    private val resourceProvider = mockk<ResourceProvider>(relaxed = true)
    private val stackProvisioningService = mockk<StackProvisioningService>(relaxed = true)

    private val useCase =
        ProvisioningUseCase(
            authRepository,
            configRepository,
            resourceProvider,
            stackProvisioningService,
        )

    private val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
    private val stackName = "locus-user-pixel-7"
    private val deviceName = "pixel-7"
    private val bucketName = "locus-logs-12345"
    private val accessKey = "runtime-access"
    private val secretKey = "runtime-secret"
    private val templateBody = "template-body"
    private val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/stack-name/guid"

    @Test
    fun `verifies bootstrap keys successfully`() =
        runBlocking {
            coEvery { resourceProvider.getStackTemplate() } returns templateBody
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf(
                            "BucketName" to bucketName,
                            "RuntimeAccessKeyId" to accessKey,
                            "RuntimeSecretAccessKey" to secretKey,
                        ),
                    ),
                )
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.FinalizingSetup }) }
        }

    @Test
    fun `fails when stack creation fails`() =
        runBlocking {
            coEvery { resourceProvider.getStackTemplate() } returns templateBody
            val error = DomainException.ProvisioningError.DeploymentFailed("Failed")
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns LocusResult.Failure(error)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(error)
        }

    @Test
    fun `fails when stack outputs are missing`() =
        runBlocking {
            coEvery { resourceProvider.getStackTemplate() } returns templateBody
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        emptyMap(), // Missing outputs
                    ),
                )

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isInstanceOf(DomainException.ProvisioningError.DeploymentFailed::class.java)
            coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `fails when identity initialization fails`() =
        runBlocking {
            coEvery { resourceProvider.getStackTemplate() } returns templateBody
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf(
                            "BucketName" to bucketName,
                            "RuntimeAccessKeyId" to accessKey,
                            "RuntimeSecretAccessKey" to secretKey,
                        ),
                    ),
                )

            val error = DomainException.AuthError.Generic(Exception("Init failed"))
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Failure(error)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(error)
             coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }

    @Test
    fun `fails when promotion fails`() =
         runBlocking {
            coEvery { resourceProvider.getStackTemplate() } returns templateBody
            coEvery { stackProvisioningService.createAndPollStack(any(), any(), any(), any()) } returns
                LocusResult.Success(
                    StackProvisioningResult(
                        stackId,
                        mapOf(
                            "BucketName" to bucketName,
                            "RuntimeAccessKeyId" to accessKey,
                            "RuntimeSecretAccessKey" to secretKey,
                        ),
                    ),
                )
            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)

            val error = DomainException.AuthError.Generic(Exception("Promote failed"))
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Failure(error)

            val result = useCase(creds, deviceName)

            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error).isEqualTo(error)
             coVerify { authRepository.updateProvisioningState(match { it is ProvisioningState.Failure }) }
        }
}
