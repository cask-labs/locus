# Report: ProvisioningWorker Testing Difficulties
**Date:** 2026-01-02
**Agent:** Jules

## Problem Description
The task involves implementing the `ProvisioningWorker` and verifying it with local unit tests using Robolectric. The `ProvisioningWorker` is hosted within the `:app` module.

The primary difficulty lies in the interaction between:
1.  **Robolectric:** Running simulated Android tests.
2.  **LocusApp (Application Class):** Initializes critical infrastructure on creation.
3.  **Tink (Security):** Initializes `AndroidKeystoreKmsClient` in `LocusApp.init`.
4.  **Hilt (Dependency Injection):** Injects members into `LocusApp`.

### Specific Failures

#### 1. Tink / Android Keystore Crash
**Error:** `java.lang.IllegalStateException` caused by `java.security.KeyStoreException` at `AndroidKeystoreKmsClient.java:98`.
**Cause:** `LocusApp` calls `TinkConfig.register()` in its `init` block. This registers the `AndroidKeystoreKmsClient`, which attempts to verify the presence of the Android Keystore. Robolectric does not fully simulate the Keystore, causing this initialization to throw an unchecked exception, crashing the test environment before the test method even runs.

#### 2. Uninitialized Property Access (Hilt)
**Error:** `kotlin.UninitializedPropertyAccessException: lateinit property authRepository has not been initialized`.
**Cause:** `LocusApp` declares `@Inject lateinit var authRepository: AuthRepository` and uses it in `onCreate()`.
In a standard Robolectric test (annotated with `@RunWith(RobolectricTestRunner::class)`), the Application class defined in the Manifest (`LocusApp`) is instantiated. However, since the test class is NOT annotated with `@HiltAndroidTest` and does not use a Hilt Test Application, Hilt injection is **not performed** on `LocusApp`. Consequently, `authRepository` remains uninitialized. When `onCreate()` is called (automatically by Robolectric), accessing this property causes a crash.

## Failed Solutions

### Attempt 1: Broad Exception Handling in LocusApp
**Strategy:** Wrap `TinkConfig.register()` in `try { ... } catch (e: Throwable) { ... }`.
**Outcome:** Failed.
**Reason:** While this prevented the Tink crash, it allowed `LocusApp` to proceed to `onCreate()`, where it immediately crashed due to the Hilt injection issue (Failure #2). Furthermore, swallowing `Throwable` is poor practice and hides legitimate production initialization errors.

### Attempt 2: Defensive Checks in LocusApp
**Strategy:** Add `if (::authRepository.isInitialized)` checks in `LocusApp.onCreate()` and lazy initialization for `workerFactory`.
**Outcome:** Partially working but rejected.
**Reason:** This pollutes production code with test-specific logic. It requires modifying the application class to handle a state (uninitialized injection) that should theoretically never happen in a properly configured production or Hilt-test environment.

### Attempt 3: Modifying EncryptionModule (Security Risk)
**Strategy:** Modify `EncryptionModule.kt` to catch exceptions during `AndroidKeysetManager` creation and return a `FakeAead` (plaintext).
**Outcome:** **Rejected (Critical).**
**Reason:** This creates a fallback path in *production code* where a failure to access the Keystore (e.g., on a weird device) results in data being stored unencrypted. This is unacceptable for security.

## Proposed Solution (Refined)
Instead of modifying `LocusApp` or `EncryptionModule` to accommodate the test environment, the test environment should be configured to bypass `LocusApp`.

**Strategy:** Use `@Config(application = android.app.Application::class)` in `ProvisioningWorkerTest`.
**Rationale:**
1.  Forces Robolectric to use the base `Application` class instead of `LocusApp`.
2.  `LocusApp.init` is never called -> No Tink Crash.
3.  `LocusApp.onCreate` is never called -> No Hilt Injection crash.
4.  `ProvisioningWorker` is tested in isolation using `TestListenableWorkerBuilder` with manually supplied mocks, so it does not depend on the Application class logic.
