# 03-task-3-domain-data-primitives-plan.md

## Purpose
Implement the core Domain and Data layer primitives and the "Tracer Bullet" feature (App Version) to validate the architecture, dependency injection, and testing pipeline.

## Prerequisites
*   **Tasks 1 & 2** (Scaffolding & Automation) are assumed to be complete.
*   Directory structure (`:core:domain`, `:core:data`, `:core:testing`) exists.
*   `libs.versions.toml` includes `kover` and `archunit`.

## Spec Alignment
*   **Domain Layer Spec:** Implements `LocusResult`, `DomainException`, and the `UseCase` pattern.
*   **Testing Spec:** Configures `Kover` (90%/80%) and `ArchUnit` rules for layer isolation.
*   **Project Structure:** Enforces strict separation between `:core:domain` (Pure Kotlin) and `:core:data` (Android).
*   **Tasking:** Implements the "Tracer Bullet" logic (`AppVersionRepository`, `GetAppVersionUseCase`).

## Implementation Steps

### Step 1: Ensure Scaffolding & Update Dependencies
*   **Action:** Verify and create basic directory structure for `core/domain` and `core/data` if missing.
*   **Action:** Update `libs.versions.toml` to include `kover` and `archunit` dependencies.
*   **Action:** Apply `kover` plugin to module build files and configure coverage rules (90% Domain, 80% Data).
*   **Verify:** Read modified build files and version catalog.

### Step 2: Create Domain Primitives (`:core:domain`)
*   **Action:** Create `LocusResult` sealed class and `DomainException`.
*   **Action:** Create `UseCase` marker interface/abstract class.
*   **Verify:** List created files in `com/locus/core/domain`.

### Step 3: Implement Tracer Bullet Domain Logic (`:core:domain`)
*   **Action:** Create `AppVersion` data class.
*   **Action:** Create `AppVersionRepository` interface.
*   **Action:** Create `GetAppVersionUseCase` class.
*   **Verify:** List created files in `com/locus/core/domain`.

### Step 4: Implement Data Layer Logic (`:core:data`)
*   **Action:** Create `AppVersionRepositoryImpl` (injecting `Context`).
*   **Action:** Create `DataModule` (Hilt) to bind the repository.
*   **Verify:** List created files in `com/locus/core/data`.

### Step 5: Implement Architecture Tests
*   **Action:** Create `DomainArchitectureTest` using `ArchUnit`.
    *   Rule: Domain classes must not depend on Android SDK.
    *   Rule: UseCase naming convention.
*   **Verify:** Check file creation.

### Step 6: Implement Unit Tests
*   **Action:** Create `GetAppVersionUseCaseTest` (Mockk).
*   **Action:** Create `AppVersionRepositoryImplTest` (Robolectric).
*   **Verify:** Check file creation.

### Step 7: Validation
*   **Action:** Run validation script (`./scripts/run_local_validation.sh`) or standard Gradle test tasks.
*   **Verify:** Ensure build succeeds, tests pass, and coverage reports are generated.

## Validation Criteria (Definition of Done)
*   [ ] Build is successful (`./gradlew assembleDebug`).
*   [ ] All Unit Tests pass (`./gradlew test`).
*   [ ] Kover reports >90% coverage for Domain and >80% for Data.
*   [ ] ArchUnit tests pass, confirming domain purity.
