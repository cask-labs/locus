# Repository Footprint: Phase 1 - Onboarding & Identity

## Files Being Added

| Path | Type | Purpose | Status |
|------|------|---------|--------|
| **Domain Layer** (`:core:domain`) | | | |
| `src/main/kotlin/.../auth/AuthRepository.kt` | Interface | State holder & Auth actions | New |
| `src/main/kotlin/.../auth/model/AuthState.kt` | Model | Sealed class for Domain state | New |
| `src/main/kotlin/.../auth/usecase/ValidateCredentialsUseCase.kt` | UseCase | "Dry Run" validation | New |
| `src/main/kotlin/.../auth/usecase/ProvisionIdentityUseCase.kt` | UseCase | Orchestrates CloudFormation (New) | New |
| `src/main/kotlin/.../auth/usecase/RecoverIdentityUseCase.kt` | UseCase | Orchestrates Linking (Recovery) | New |
| **Data Layer** (`:core:data`) | | | |
| `src/main/kotlin/.../auth/RealAuthRepository.kt` | Repository | Implementation with StateFlow | New |
| `src/main/kotlin/.../auth/local/EncryptedPrefsDataSource.kt` | DataSource | Secure Key Storage | New |
| `src/main/kotlin/.../auth/remote/CloudFormationClient.kt` | DataSource | AWS SDK Wrapper | New |
| `src/main/kotlin/.../auth/worker/ProvisioningWorker.kt` | Worker | WorkManager Worker for long tasks | New |
| `src/main/assets/locus-stack.yaml` | Asset | CloudFormation Template (Standard) | New |
| `src/main/assets/locus-access-stack.yaml` | Asset | CloudFormation Template (Recovery) | New |
| `src/main/assets/locus-admin.yaml` | Asset | CloudFormation Template (Admin) | New |
| **App Layer** (`:app`) | | | |
| `src/main/kotlin/.../onboarding/OnboardingViewModel.kt` | ViewModel | Glue between UI and Repo | New |
| `src/main/kotlin/.../onboarding/OnboardingScreen.kt` | UI | Main flow coordinator | New |
| `src/main/kotlin/.../onboarding/screens/CredentialInput.kt` | UI | Input Form | New |
| `src/main/kotlin/.../onboarding/screens/ProvisioningProgress.kt` | UI | Log-style progress view | New |
| `src/main/kotlin/.../onboarding/screens/SuccessTrap.kt` | UI | Success/Failure trap | New |

**Approximate size:** ~1200 lines of code (including tests).

## Module Structure

```
:core:domain
  /auth
    AuthRepository.kt
    /model
      AuthCredentials.kt
      AuthState.kt
    /usecase
      ValidateCredentialsUseCase.kt
      ProvisionIdentityUseCase.kt
      RecoverIdentityUseCase.kt

:core:data
  /auth
    RealAuthRepository.kt
    /local
      EncryptedPrefsDataSource.kt
    /remote
      CloudFormationClient.kt
    /worker
      ProvisioningWorker.kt

:app
  /features/onboarding
    OnboardingViewModel.kt
    OnboardingScreen.kt
    /components
      ...
```

## Configuration Snippet

**AndroidManifest.xml (`:core:data` / `:app`)**
```xml
<!-- WorkManager is auto-configured via App Startup, but we may need -->
<!-- specific configuration for expedited work if customizing initialization -->
```
