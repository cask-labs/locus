# Bill of Materials: Phase 1

This document lists the specific classes, files, and artifacts required to implement the Phase 1 Architecture.

## 1. Domain Layer (`:core:domain`)

### Models
*   `model/AuthState.kt` (Sealed Class: Uninitialized, SetupPending, Provisioning, Authenticated, Failure)
*   `model/BootstrapCredentials.kt` (Data Class: ak, sk, token)
*   `model/RuntimeCredentials.kt` (Data Class: ak, sk, bucket, region, role)
*   `model/ProvisioningLog.kt` (Data Class: timestamp, message, type)

### Repositories
*   `repository/AuthRepository.kt` (Interface)

### Use Cases
*   `usecase/ValidateCredentialsUseCase.kt`
*   `usecase/ProvisionIdentityUseCase.kt`
*   `usecase/RecoverIdentityUseCase.kt`
*   `usecase/GetProvisioningLogsUseCase.kt` (For the "Console View")

### Errors
*   `error/AuthErrors.kt` (Sealed Class: InvalidCredentials, ProvisioningFailed, etc.)

## 2. Data Layer (`:core:data`)

### Repository Implementation
*   `repository/AuthRepositoryImpl.kt`
    *   Dependencies: `EncryptedSharedPreferences`, `CloudFormationClient`, `S3Client`.

### Data Sources
*   `source/aws/CloudFormationClient.kt`
*   `source/aws/S3Client.kt` (Specifically for `ListBuckets` and `HeadBucket`)
*   `source/local/AuthPreferences.kt` (Wrapper for SharedPreferences)

### CloudFormation
*   `assets/locus-stack.yaml` (The "Standard" Device Stack Template)
*   `assets/locus-access-stack.yaml` (The "Recovery" Satellite Stack Template)
*   `assets/locus-admin.yaml` (The "Admin" Template)

## 3. UI Layer (`:app` / `:feature:onboarding`)

### ViewModels
*   `OnboardingViewModel.kt`
    *   Exposes `uiState` mapped from `AuthRepository`.

### Screens (Compose)
*   `OnboardingScreen.kt` (Scaffold)
*   `screens/WelcomeScreen.kt`
*   `screens/CredentialInputScreen.kt`
*   `screens/SetupChoiceScreen.kt` (New vs Recover)
*   `screens/ProvisioningScreen.kt` (The "Console View" with progress)
*   `screens/RecoverySelectionScreen.kt` (List of buckets)
*   `screens/SuccessScreen.kt`

### Worker
*   `worker/ProvisioningWorker.kt`
    *   WorkManager Worker implementation.
    *   Handles long-running deployment and notifications.

## 4. Testing Artifacts

### Test Doubles
*   `FakeAuthRepository.kt`
*   `FakeCloudFormationClient.kt` (Simulates Stack events)

### Integration Tests
*   `ProvisioningFlowTest.kt` (Robolectric test of the Worker/Repo interaction)
