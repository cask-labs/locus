# Testing Strategy Specification (Phase 7)

**Related Requirements:** [Testing Rules](../../agents/rules/testing.md), [Advanced Validation](../operations/advanced_validation.md)

This document defines the strict testing standards, libraries, and patterns required for the Locus Android application.

## 1. Technology Stack

All tests must use the following libraries (defined in `libs.versions.toml`):

*   **Unit Logic:** `Mockk` (Kotlin-native mocking).
*   **Assertions:** `Truth` (Fluent assertions).
*   **Coroutines/Flows:** `Turbine` (Flow collection).
*   **Integration/Service:** `Robolectric` (Headless Android environment).
*   **UI Tests:** `Compose UI Test` (JUnit 4).

## 2. Unit Testing Strategy

### 2.1. Domain Layer (Pure Kotlin)
*   **Scope:** Use Cases, Models, Repository Interfaces (Fakes).
*   **Rules:**
    *   **No Mocks for Data Classes:** Instantiate real Models.
    *   **Mock Repositories:** Use `mockk<LocationRepository>()` to define behavior.
    *   **Test Coroutines:** Use `runTest` from `kotlinx-coroutines-test`.

**Example (UseCase Test):**
```kotlin
@Test
fun `performSync returns failure when battery is critical`() = runTest {
    // Given
    val deviceState = mockk<DeviceStateRepository>()
    coEvery { deviceState.getCurrentSystemState() } returns SystemState.CriticalBattery(5)
    val useCase = PerformSyncUseCase(..., deviceState)

    // When
    val result = useCase(SyncType.REGULAR)

    // Then
    assertThat(result).isInstanceOf(Result.Failure::class.java)
}
```

### 2.2. ViewModel Testing
*   **Scope:** ViewModels in `:app` or features.
*   **Tool:** `Turbine` for observing StateFlows.
*   **Pattern:** Input Event -> Assert State Emission.

**Example (ViewModel Flow):**
```kotlin
@Test
fun `tracking status updates ui state`() = runTest {
    val viewModel = DashboardViewModel(fakeRepo)

    viewModel.uiState.test {
        assertThat(awaitItem()).isEqualTo(DashboardUiState.Loading)
        fakeRepo.emitStatus(Status.Active)
        assertThat(awaitItem()).isEqualTo(DashboardUiState.Active)
    }
}
```

## 3. Integration Testing Strategy

### 3.1. Service & Worker Testing
*   **Tool:** `Robolectric`.
*   **Why:** Allows testing `ForegroundService` lifecycle and `WorkManager` constraints without a physical device.
*   **Config:** Annotate class with `@RunWith(RobolectricTestRunner::class)`.

### 3.2. Database Testing
*   **Tool:** `Room` in-memory database + `Robolectric`.
*   **Scope:** DAO queries, Entity relationships.
*   **Rule:** Always use `allowMainThreadQueries()` in tests for simplicity, unless specifically testing concurrency.

## 4. Test Doubles Pattern

Prefer **Fakes** over Mocks for complex shared dependencies.

*   **Definition:** A lightweight implementation of an interface (e.g., `FakeLocationRepository` using an in-memory `MutableList`).
*   **Benefit:** Reduces setup boilerplate in every test file.
*   **Location:** Store fakes in a `shared-test` source set if used across modules.

## 5. Naming Conventions

*   **Class:** `[ClassUnderTest]Test` (e.g., `PerformSyncUseCaseTest`).
*   **Method:** Backtick syntax describing the behavior.
    *   `methodName_condition_expectedResult`
    *   `returns failure when network is unavailable` (Preferred for readabilty).
