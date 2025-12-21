# Repository Delta: Phase 0

## File Change Table

| Path | Kind | Purpose | Reuse/New |
| :--- | :--- | :--- | :--- |
| `build.gradle.kts` | Build | Root Project Config | New |
| `settings.gradle.kts` | Build | Module Inclusion | New |
| `gradle/libs.versions.toml` | Build | Version Catalog | New |
| `gradle/wrapper/*` | Build | Gradle Wrapper (8.5) | New |
| `app/build.gradle.kts` | Build | App Module Config | New |
| `core/domain/build.gradle.kts` | Build | Domain Module Config | New |
| `core/data/build.gradle.kts` | Build | Data Module Config | New |
| `core/testing/build.gradle.kts` | Build | Testing Module Config | New |
| `app/src/main/AndroidManifest.xml` | Config | App Manifest | New |
| `app/src/main/java/com/locus/android/LocusApp.kt` | App | Hilt Application Class | New |
| `app/src/main/java/com/locus/android/di/AppModule.kt` | DI | Hilt Module (Repositories) | New |
| `app/src/main/java/com/locus/android/features/dashboard/DashboardScreen.kt` | UI | "Hello World" + Version | New |
| `app/src/main/java/com/locus/android/MainActivity.kt` | UI | Entry Point | New |
| `core/domain/src/main/java/com/locus/core/domain/LocusResult.kt` | Domain | Result Wrapper | New |
| `core/domain/src/main/java/com/locus/core/domain/usecase/GetAppVersionUseCase.kt` | Domain | Tracer Bullet Logic | New |
| `core/domain/src/main/java/com/locus/core/domain/repository/AppVersionRepository.kt` | Domain | Tracer Bullet Interface | New |
| `core/data/src/main/java/com/locus/core/data/repository/AppVersionRepositoryImpl.kt` | Data | Tracer Bullet Impl | New |
| `scripts/setup_ci_env.sh` | Script | Env Bootstrap | New |
| `scripts/run_local_validation.sh` | Script | Local CI Wrapper | New |
| `scripts/verify_security.sh` | Script | Security Scanner | New |
| `scripts/build_artifacts.sh` | Script | Build Wrapper | New |
| `scripts/requirements.txt` | Config | Python Deps | New |
| `.github/workflows/validation.yml` | CI | GitHub Actions | New |

**Estimated LOC:** ~500 LOC (mostly config/boilerplate).

## Feature Tree

```
com.locus
├── android (App)
│   ├── di
│   │   └── AppModule.kt
│   ├── features
│   │   └── dashboard
│   │       ├── DashboardScreen.kt
│   │       └── DashboardViewModel.kt
│   ├── LocusApp.kt
│   └── MainActivity.kt
├── core
│   ├── domain
│   │   ├── model
│   │   ├── repository
│   │   │   └── AppVersionRepository.kt
│   │   ├── result
│   │   │   └── LocusResult.kt
│   │   └── usecase
│   │       └── GetAppVersionUseCase.kt
│   └── data
│       └── repository
│           └── AppVersionRepositoryImpl.kt
```

---

# Spec Alignment Matrix

| Spec Requirement (Roadmap Phase 0) | Component(s) | Notes |
| :--- | :--- | :--- |
| **Project Scaffolding** (Gradle, Kotlin DSL) | Root `build.gradle.kts`, `settings.gradle.kts` | Using strict Kotlin DSL. |
| **Modules** (`:app`, `:core:domain`, etc.) | `settings.gradle.kts`, Module `build.gradle.kts` | Strict isolation enforced. |
| **Build Variants** (`standard`, `foss`) | `app/build.gradle.kts` (productFlavors) | Configured with dimension "distribution". |
| **Versioning** (Git Tag based) | `app/build.gradle.kts` | Logic to parse `git describe` and `rev-list`. |
| **Validation Scripts** (`setup_ci_env`, etc.) | `scripts/` directory | 1:1 mapping to `automation_scripts_spec.md`. |
| **CI Pipeline** (GitHub Actions) | `.github/workflows/validation.yml` | Maps to `ci_pipeline_spec.md`. |
| **Dependency Injection** (Hilt) | `LocusApp.kt`, `AppModule.kt` | Basic graph setup. |
| **Domain Base Types** | `LocusResult.kt` | Sealed class for error handling. |
| **"Hello World" UI** | `DashboardScreen.kt` | Implemented as "Tracer Bullet". |
