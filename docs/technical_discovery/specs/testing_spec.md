# Testing Strategy Specification

**Related Requirements:** [Testing Rules](../../agents/rules/testing.md), [Advanced Validation](advanced_validation_spec.md), [Automation Scripts](automation_scripts_spec.md)

This document defines the strict testing standards, libraries, and patterns required for the Locus Android application.

## 1. Technology Stack

All tests must use the following libraries (defined in `libs.versions.toml`):

*   **Unit Logic:** `Mockk` (Kotlin-native mocking).
*   **Assertions:** `Truth` (Fluent assertions).
*   **Coroutines/Flows:** `Turbine` (Flow collection).
*   **Integration/Service:** `Robolectric` (Headless Android environment).
*   **UI Tests:** `Compose UI Test` (JUnit 4).
*   **System UI Tests:** `UiAutomator` (Permissions, Notifications).
*   **Code Coverage:** `Kover` (Kotlin JaCoCo wrapper).
*   **Architecture Validation:** `ArchUnit` (Java/Kotlin structure assertion).

## 2. Project Structure & Organization

The project adheres to a strict separation of test types to optimize execution speed and reliability.

| Test Type | Source Set | Execution Environment | Scope |
| :--- | :--- | :--- | :--- |
| **Unit Tests** | `src/test/kotlin` | Local JVM | Domain Logic, ViewModels, Pure Kotlin classes. |
| **Integration Tests** | `src/test/kotlin` | Local JVM (Robolectric) | Services, Workers, Room DAOs, BroadcastReceivers. |
| **Instrumented Tests** | `src/androidTest/kotlin` | Android Device / Emulator | System UI, Permissions, Hardware Sensors, E2E Flows. |

### 2.1. Shared Test Fixtures
Reusable Fakes, Mocks, and Test Utilities (e.g., `MainDispatcherRule`) must be located in the **`:core:testing`** module.
*   **Usage:** Other modules (`:app`, `:core:data`) depend on this via `testImplementation project(":core:testing")`.
*   **Benefit:** Ensures the Domain Layer Fakes are identical across all test suites.

## 3. Test Tiers (Validation Pipeline)

This strategy directly maps to the Validation Pipeline Tiers defined in `automation_scripts_spec.md`.

| Tier | Description | Included Tests |
| :--- | :--- | :--- |
| **Tier 2** | Local Logic Verification | All tests in `src/test/kotlin` (Unit + Robolectric). |
| **Tier 5** | Device Farm / Hardware | All tests in `src/androidTest/kotlin` (Instrumentation). |

## 4. Unit Testing Strategy (Tier 2)

### 4.1. Domain Layer (Pure Kotlin)
*   **Scope:** Use Cases, Models, Repository Interfaces (Fakes).
*   **Coverage Target:** **90%** (Strict).
*   **Rules:**
    *   **No Mocks for Data Classes:** Instantiate real Models (`LocationPoint`, `LogEntry`).
    *   **Mock Repositories:** Use `mockk<LocationRepository>()` only if a Fake is too complex; prefer Fakes from `:core:testing`.
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
    assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
}
```

### 4.2. Data Layer (Repositories)
*   **Scope:** Repository Implementations, Mappers, Data Sources.
*   **Coverage Target:** **80%**.
*   **Rules:**
    *   **Mock Clients:** Mock the *Wrapper* (e.g., `S3Client`, `LocationSource`), not the low-level HTTP/Bluetooth stack.
    *   **Verify Mappers:** Ensure DTOs map correctly to Domain Models.

### 4.3. ViewModel Testing
*   **Scope:** ViewModels in `:app` or features.
*   **Coverage Target:** **70%**.
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

## 5. Integration Testing Strategy (Tier 2 - Local)

These tests run on the JVM using **Robolectric** to simulate the Android framework.

### 5.1. Service & Worker Testing
*   **Scope:** `TrackerService`, `SyncWorker`, `WatchdogWorker`.
*   **Config:** `@RunWith(RobolectricTestRunner::class)`.
*   **Pattern:**
    1.  Start Service/Worker.
    2.  Simulate Environment (e.g., `ShadowApplication`).
    3.  Verify Invariants (Service started, Work enqueued).

### 5.2. Database Testing
*   **Scope:** Room DAOs.
*   **Tool:** `Room` in-memory database + `Robolectric`.
*   **Rule:** Use `allowMainThreadQueries()` for simplicity in tests.

### 5.3. Traffic Guardrail Integration Test
*   **Mandatory Test Suite:** `TrafficGuardrailIntegrationTest`.
*   **Objective:** Verify that the "Defense in Depth" (Explicit Class) guardrail is correctly blocking calls when the quota is exceeded.
*   **Methodology:**
    1.  Mock `TrafficGuardrail` to simulate an "Over Quota" state.
    2.  Inject this mock into every Repository (`LocationRepository`, `LogRepository`, `AuthRepository`).
    3.  Call **every** public network-accessing method in these repositories.
    4.  **Assertion:** Verify that each call throws `QuotaExceededException` (or `LocusResult.Failure` with specific cause). If any call successfully delegates to the network client, the test fails.

## 6. System & UI Testing Strategy (Tier 5 - Device)

These tests require a real device or emulator and are executed via **Device Farm**.

### 6.1. Compose UI Tests
*   **Scope:** Critical User Flows (Dashboard, Map).
*   **Tool:** `ComposeTestRule`.
*   **Pattern:** Use `testTag` modifiers for stable element lookup.

**Example:**
```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun startTrackingUpdatesStatus() {
    composeTestRule.setContent { DashboardScreen(...) }

    composeTestRule.onNodeWithTag("StartButton").performClick()
    composeTestRule.onNodeWithText("Active").assertIsDisplayed()
}
```

### 6.2. System Integration (UiAutomator)
*   **Scope:** Permissions, Notifications, Home Screen interaction.
*   **Tool:** `UiAutomator`.
*   **Why:** Compose tests cannot interact with system dialogs.

**Example (Permission Grant):**
```kotlin
val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
// Trigger permission dialog
device.findObject(By.text("While using the app")).click()
```

## 7. Test Doubles Pattern

Prefer **Fakes** over Mocks for complex shared dependencies.

*   **Definition:** A lightweight implementation of an interface (e.g., `FakeLocationRepository` using an in-memory `MutableList`).
*   **Location:** Store fakes in **`:core:testing`**.
*   **Benefit:** Reduces setup boilerplate and ensures tests behave consistently.

## 8. Naming Conventions

*   **Class:** `[ClassUnderTest]Test` (e.g., `PerformSyncUseCaseTest`).
*   **Method:** Backtick syntax describing the behavior.
    *   `returns failure when network is unavailable` (Preferred for readability).

## 9. Architecture Validation (ArchUnit)

Architecture tests enforce structural rules to maintain modularity and prevent circular dependencies.

*   **Scope:** All modules.
*   **Tool:** `ArchUnit` (via JUnit 5).
*   **Rules Enforced:**
    1.  **Layer Isolation:** Classes in `:core:domain` must be **Pure Kotlin** and must not depend on `android.*` packages.
    2.  **Feature Isolation:** Feature modules (e.g., `:app:features:dashboard`) must not depend on each other. Interaction must occur via the Domain Layer or Navigation.
    3.  **Data Layer Safety:** `Room` Entities (Data Layer) must not be exposed to the UI Layer. ViewModels must consume Domain Models mapped by Repositories.
    4.  **Network Safety:** Any class in `:core:data` that injects `S3Client` or `CloudFormationClient` **MUST** also inject `TrafficGuardrail`. This automated check prevents human error in omitting the quota check.
