package com.locus.core.data.repository

import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.domain.model.auth.ProvisioningState
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AuthRepositoryImplTest {
    private val awsClientFactory = mockk<AwsClientFactory>(relaxed = true)
    private val secureStorage = mockk<SecureStorageDataSource>(relaxed = true)
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val repository =
        AuthRepositoryImpl(
            awsClientFactory,
            secureStorage,
            scope,
            workManager,
        )

    @Test
    fun `updateProvisioningState correctly accumulates history`() =
        runBlocking {
            // Initial State
            repository.updateProvisioningState(ProvisioningState.Working("Step 1"))
            var state = repository.getProvisioningState().first()
            assertThat(state).isInstanceOf(ProvisioningState.Working::class.java)
            assertThat((state as ProvisioningState.Working).currentStep).isEqualTo("Step 1")
            assertThat(state.history).isEmpty()

            // Update 1
            repository.updateProvisioningState(ProvisioningState.Working("Step 2"))
            state = repository.getProvisioningState().first()
            assertThat((state as ProvisioningState.Working).currentStep).isEqualTo("Step 2")
            assertThat(state.history).containsExactly("Step 1")

            // Update 2
            repository.updateProvisioningState(ProvisioningState.Working("Step 3"))
            state = repository.getProvisioningState().first()
            assertThat((state as ProvisioningState.Working).currentStep).isEqualTo("Step 3")
            assertThat(state.history).containsExactly("Step 1", "Step 2").inOrder()
        }

    @Test
    fun `updateProvisioningState respects circular buffer limit`() =
        runBlocking {
            // Fill history to limit
            val limit = ProvisioningState.MAX_HISTORY_SIZE
            repository.updateProvisioningState(ProvisioningState.Working("Step 0"))

            for (i in 1..limit) {
                repository.updateProvisioningState(ProvisioningState.Working("Step $i"))
            }

            var state = repository.getProvisioningState().first() as ProvisioningState.Working
            assertThat(state.currentStep).isEqualTo("Step $limit")
            assertThat(state.history).hasSize(limit) // History should be size 100
            assertThat(state.history.first()).isEqualTo("Step 0")
            assertThat(state.history.last()).isEqualTo("Step ${limit - 1}")

            // Add one more to trigger eviction
            repository.updateProvisioningState(ProvisioningState.Working("Overflow"))

            state = repository.getProvisioningState().first() as ProvisioningState.Working
            assertThat(state.currentStep).isEqualTo("Overflow")
            assertThat(state.history).hasSize(limit)
            // "Step 0" should be evicted
            assertThat(state.history.first()).isEqualTo("Step 1")
            assertThat(state.history.last()).isEqualTo("Step $limit")
        }

    @Test
    fun `updateProvisioningState resets history on non-working state transition`() =
        runBlocking {
            repository.updateProvisioningState(ProvisioningState.Working("Step 1"))
            repository.updateProvisioningState(ProvisioningState.Working("Step 2"))

            // Transition to Success
            repository.updateProvisioningState(ProvisioningState.Success)
            var state = repository.getProvisioningState().first()
            assertThat(state).isInstanceOf(ProvisioningState.Success::class.java)

            // Transition back to Working (should start fresh)
            repository.updateProvisioningState(ProvisioningState.Working("New Start"))
            state = repository.getProvisioningState().first() as ProvisioningState.Working
            assertThat(state.currentStep).isEqualTo("New Start")
            assertThat(state.history).isEmpty()
        }
}
