# Exploring Architectures: Phase 1 - Onboarding & Identity

## Goals and Boundaries

**What we're building:**
The foundational Onboarding flow that establishes User Identity, validates credentials, and provisions AWS infrastructure (S3, IAM) for the first time.

**What's included:**
- Credential Entry & Validation (AWS Session Tokens).
- Infrastructure Provisioning (CloudFormation Stack deployment).
- "Setup Trap" (Ensuring the user cannot bypass onboarding).
- Security Lifecycle (Swapping Bootstrap Keys for Runtime Keys).
- Recovery Flow (Linking existing buckets).

**Rules and dependencies:**
- **R1.600:** Provisioning must be a "visible background task" (Resilience).
- **R1.1000:** Failures must not auto-delete resources (Fail-Safe).
- **Security:** "Bootstrap Keys" must be discarded after use.

---

## Path A: ViewModel Orchestration (The "Lightweight" Approach)

**Description:**
The `OnboardingViewModel` drives the entire process using Kotlin Coroutines. It calls the `ProvisioningUseCase` directly. State is held in the ViewModel's memory and saved to `SharedPreferences` only upon major milestones (e.g., "Complete").

**Design:**
- **Inbound:** User clicks "Start Setup".
- **Outbound:** ViewModel calls `ProvisioningUseCase`.
- **Concurrency:** `viewModelScope.launch`.
- **State:** `MutableStateFlow<OnboardingUiState>` in ViewModel.
- **Resilience:** Relies on the user keeping the app open.

**Strengths:**
- Simple to implement.
- Minimal boilerplate (Standard MVVM).

**Weaknesses:**
- **Low Resilience:** If the app is backgrounded and the OS kills the process, the provisioning aborts mid-way.
- **State Loss:** Detailed progress logs are lost on process death unless manually persisted.
- **Violation Risk:** Might violate R1.600 (Resilience against termination).

---

## Path B: Domain State Machine + Foreground Service (The "Robust" Approach)

**Description:**
A dedicated `ProvisioningService` (Foreground Service) executes the logic, ensuring the process survives backgrounding. The state is managed by a singleton `ProvisioningRepository` (or `AuthRepository`) which persists the detailed "Step" and "Logs" to disk (`EncryptedSharedPreferences` or local file) in real-time.

**Design:**
- **Inbound:** UI starts `ProvisioningService`.
- **Outbound:** Service calls `ProvisioningUseCase`.
- **Concurrency:** `Service` lifecycle + `CoroutineScope`.
- **State:** `AuthRepository` holds a persisted `StateFlow<ProvisioningState>`.
- **Resilience:** Foreground Notification keeps the process alive; Disk persistence allows resumption after crash.

**Strengths:**
- **High Resilience:** Best guarantee of completion (meets R1.600).
- **Tombstoning:** If the app crashes, the "Log" is saved on disk, so the user sees exactly where it failed upon reopening (meets "Setup Trap" & Error Handling needs).
- **Separation:** UI is strictly a viewer of the Repository state.

**Weaknesses:**
- Higher engineering effort (Service boilerplate, notification management).
- Complexity in handling Service <-> UI communication (though Repository makes this easier).

---

## Path C: WorkManager (The "Async" Approach)

**Description:**
Offload the task to Android's `WorkManager`. The Provisioning logic runs as a `Worker`.

**Design:**
- **Inbound:** UI queues a `OneTimeWorkRequest`.
- **State:** `WorkInfo` (Progress Data).

**Strengths:**
- Native Android solution for "guaranteed execution".

**Weaknesses:**
- **Security Risk:** Passing sensitive "Bootstrap Keys" to a Worker requires serializing them into `Data` (persisted to disk by OS), which is less secure than keeping them in memory/EncryptedSharedPreferences.
- **Latency:** WorkManager execution is not immediate; it's designed for "deferrable" work.
- **Feedback:** passing granular "Log" updates from a Worker to UI is clunky (intermediate progress updates).

---

## Comparison Grid

| Dimension | Path A (ViewModel) | Path B (Service + Repo) | Path C (WorkManager) |
|-----------|--------------------|-------------------------|----------------------|
| **Resilience (R1.600)** | Low | **High** | High |
| **Security (Keys)** | High (Memory) | **High (Memory/Encrypted)** | Low (Serialized Input) |
| **UX Feedback (Logs)** | Instant | **Instant** | Delayed/Batched |
| **Engineering Effort** | Low | **Medium** | Medium |
| **Resume/Trap Capability**| Low | **High** | Medium |

---

## Recommendation

**Path B (Domain State Machine + Foreground Service)** is strongly recommended.
It creates the most robust user experience for a critical, one-time operation. It ensures that if the detailed CloudFormation deployment takes 2-3 minutes, the user can background the app without killing the process, and if they do force-quit, the specific error log is waiting for them when they return.
