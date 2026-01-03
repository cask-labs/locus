package com.locus.core.data.repository

import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.domain.model.auth.ProvisioningState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {
    private val awsClientFactory = mockk<AwsClientFactory>(relaxed = true)
    private val secureStorage = mockk<SecureStorageDataSource>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val repository =
        AuthRepositoryImpl(
            awsClientFactory,
            secureStorage,
            testScope,
            workManager,
        )

    @Test
    fun `accumulates history when transitioning between working states`() =
        runTest(testDispatcher) {
            // Given initial state
            repository.updateProvisioningState(ProvisioningState.Working("Step 1"))

            // When updating to Step 2
            repository.updateProvisioningState(ProvisioningState.Working("Step 2"))

            // Then history should contain Step 1
            val state2 = repository.getProvisioningState().first() as ProvisioningState.Working
            assertThat(state2.currentStep).isEqualTo("Step 2")
            assertThat(state2.history).containsExactly("Step 1")

            // When updating to Step 3
            repository.updateProvisioningState(ProvisioningState.Working("Step 3"))

            // Then history should contain Step 1 and Step 2
            val state3 = repository.getProvisioningState().first() as ProvisioningState.Working
            assertThat(state3.currentStep).isEqualTo("Step 3")
            assertThat(state3.history).containsExactly("Step 1", "Step 2").inOrder()
        }

    @Test
    fun `resets history when transitioning from Idle`() =
        runTest(testDispatcher) {
            repository.updateProvisioningState(ProvisioningState.Idle)
            repository.updateProvisioningState(ProvisioningState.Working("Step 1"))

            val state = repository.getProvisioningState().first() as ProvisioningState.Working
            assertThat(state.history).isEmpty()
        }

    @Test
    fun `respects max history size`() =
        runTest(testDispatcher) {
            // Fill history to limit
            var currentHistory = (1..ProvisioningState.MAX_HISTORY_SIZE).map { "Step $it" }

            // Hack: manually set state with full history to avoid 100 calls
            // Since we can't easily access the mutable state directly without reflection or just calling update loop.
            // Let's just call update 105 times, it's fast in unit test.

            repository.updateProvisioningState(ProvisioningState.Working("Step 0"))

            for (i in 1..ProvisioningState.MAX_HISTORY_SIZE + 5) {
                repository.updateProvisioningState(ProvisioningState.Working("Step $i"))
            }

            val state = repository.getProvisioningState().first() as ProvisioningState.Working
            assertThat(state.history).hasSize(ProvisioningState.MAX_HISTORY_SIZE)

            // Should contain the last 100 items (Step 6 to Step 105, since current is Step 106?)
            // If we did 105 updates:
            // 0 -> 1 (hist: 0)
            // ...
            // Final is Step 105. History should end with Step 104.
            // Let's verify specific content.

            val lastHistoryItem = state.history.last()
            val expectedLastHistoryItem = "Step ${ProvisioningState.MAX_HISTORY_SIZE + 4}" // Step 104
            assertThat(lastHistoryItem).isEqualTo(expectedLastHistoryItem)

            val firstHistoryItem = state.history.first()
            val expectedFirstHistoryItem = "Step 5" // Since 0, 1, 2, 3, 4 are evicted (5 items evicted)
            assertThat(firstHistoryItem).isEqualTo(expectedFirstHistoryItem)
        }
}
