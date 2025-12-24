# Spec Alignment: Phase 1 - Onboarding & Identity

| Requirement ID | Requirement Summary | Implementation Component |
|----------------|---------------------|--------------------------|
| **R1.100** | Validate credentials ("Dry Run") | `ValidateCredentialsUseCase.kt` |
| **R1.200** | Fail on invalid dry run | `ValidateCredentialsUseCase.kt` |
| **R1.300** | Require Session Token | `CredentialInput.kt` (Validation) |
| **R1.400** | Pre-fill Device Name | `OnboardingViewModel.kt` |
| **R1.500** | Check name uniqueness | `ProvisionResourcesUseCase.kt` (S3 HeadBucket check) |
| **R1.600** | Visible background task | `ProvisioningService.kt` (Foreground Notification) |
| **R1.700** | Use Bootstrap Keys for deployment | `CloudFormationClient.kt` |
| **R1.800** | Generate Runtime User | `locus-stack.yaml` (IAM User Resource) |
| **R1.900** | Swap Keys (Bootstrap -> Runtime) | `RealAuthRepository.kt` |
| **R1.1000**| Fail-safe (No auto-delete) | `ProvisionResourcesUseCase.kt` (Error Handling) |
| **R1.1100**| List buckets for recovery | `RecoverAccountUseCase.kt` |
| **R1.1200**| "No stores found" message | `OnboardingScreen.kt` |
| **R1.1300**| Create new Runtime User (Recovery) | `RecoverAccountUseCase.kt` (IAM CreateUser) |
| **R1.1400**| Generate unique `device_id` | `EncryptedPrefsDataSource.kt` |
| **R1.1500**| Lazy Sync (Inventory) | Deferred to Phase 3 (Skeleton stub only) |
| **R1.1600**| Success Screen (Manual Confirm) | `OnboardingScreen.kt` |
| **R1.1700**| Clear stack -> Dashboard | `MainActivity.kt` (NavGraph) |
| **R1.1800**| Start Tracking/Watchdog | `DashboardViewModel.kt` (On Init) |
| **R1.1900**| Setup Trap (Resume state) | `RealAuthRepository.kt` (Persistent State) |
| **R1.2000**| Admin Template support | `locus-admin.yaml` (Asset) |

**Coverage:** 95% (Lazy Sync is architectural stub only).
