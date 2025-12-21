# Phase 0 Tasking: Foundation & Tracer Bullet

## Task 1: Project Scaffolding & Build Infrastructure

**Description:**
Establish the root Gradle configuration, Version Catalog, and module structure. This sets up the build environment without any application logic, ensuring that `./gradlew projects` lists all modules correctly and the project compiles.

**Requirements:**
-   **Version Catalog (`gradle/libs.versions.toml`):** Define all dependencies including Android, Compose, Hilt, Coroutines, JUnit, Mockk, Truth, Turbine, Robolectric, and **Kover** (for coverage) + **ArchUnit**.
-   **Root Build:** Configure `build.gradle.kts` with common plugins and clean tasks.
-   **Settings:** Configure `settings.gradle.kts` to include `:app`, `:core:domain`, `:core:data`, and `:core:testing`.
-   **Modules:** Create `build.gradle.kts` for each module with strict layer isolation:
    -   `:core:domain`: Pure Kotlin (no Android dependencies).
    -   `:core:data`: Android Library, depends on Domain.
    -   `:core:testing`: Test Fixtures.
    -   `:app`: Android Application, depends on Data and Domain.
-   **Wrapper:** Ensure Gradle 8.5+ wrapper is present.

**Validation:**
```bash
./gradlew projects
./gradlew assembleDebug
```

---

## Task 2: Automation Scripts (The Validation Layer)

**Description:**
Implement the "Validation Pipeline" scripts defined in the deep dive. These scripts standardize the developer workflow and CI environment.

**Requirements:**
-   **Scripts:** Create the following in `scripts/`:
    -   `setup_ci_env.sh`: Installs Python requirements, checks Java version.
    -   `run_local_validation.sh`: Runs Lint, Unit Tests, and ArchUnit tests.
    -   `verify_security.sh`: Placeholder/Basic check for secrets.
    -   `build_artifacts.sh`: Builds AAB/APK.
-   **Config:** Create `scripts/requirements.txt` for Python dependencies.
-   **CI:** Create `.github/workflows/validation.yml` that invokes these scripts.

**Validation:**
```bash
chmod +x scripts/*.sh
./scripts/setup_ci_env.sh
```

---

## Task 3: Domain & Data Primitives (Tracer Bullet Logic)

**Description:**
Implement the core architectural components and the "Tracer Bullet" feature logic (App Version) to validate the layer communication.

**Requirements:**
-   **Domain Primitives:**
    -   Create `LocusResult` sealed class hierarchy (`Success`, `Failure`).
    -   Define the `UseCase` abstraction.
-   **Tracer Bullet Domain:**
    -   Create `AppVersionRepository` interface in `:core:domain`.
    -   Create `GetAppVersionUseCase` in `:core:domain`.
-   **Data Implementation:**
    -   Create `AppVersionRepositoryImpl` in `:core:data`.
-   **Testing:**
    -   Configure **Kover** to enforce 90% (Domain) / 80% (Data) coverage.
    -   Add Unit Tests for UseCase and Repository using `Mockk` and `Truth`.
    -   Add `ArchUnit` tests to enforce domain purity and dependency rules.

**Validation:**
```bash
./scripts/run_local_validation.sh
```

---

## Task 4: Application Entry Point (Hello World UI)

**Description:**
Wire up the Android Application layer with Hilt, create the main entry point, and display the "Hello World" UI with the version retrieved from the Domain layer.

**Requirements:**
-   **Hilt Setup:** Create `LocusApp.kt` (`@HiltAndroidApp`) and `AppModule.kt` (Provides Repositories).
-   **Manifest:** Create `AndroidManifest.xml` with proper permissions/activities.
-   **UI:**
    -   Create `MainActivity.kt` (`@AndroidEntryPoint`).
    -   Create `DashboardScreen.kt` displaying "Hello Locus vX.X.X".
-   **Build Config:** Ensure `app/build.gradle.kts` handles `versionCode`/`versionName` generation from Git.

**Validation:**
```bash
./scripts/run_local_validation.sh
./scripts/build_artifacts.sh
```
