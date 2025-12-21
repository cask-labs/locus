# Architecture Discovery: Phase 0 (Foundation & Validation)

## Objective and Scope

**Objective:** Establish the Multi-Module Android project structure, configure the Build & Validation infrastructure (Gradle + Scripts), and prove the application can launch ("Hello World").

**In Scope:**
- Gradle Project Initialization (Kotlin DSL, Version Catalogs).
- Multi-Module Structure (`:app`, `:core:domain`, `:core:data`, `:core:testing`).
- Validation Scripts (CI/CD Local Wrappers).
- Basic Dependency Injection (Hilt).
- Release Configuration (Signing, Flavors).

**Out of Scope:**
- Feature implementation (Onboarding, Tracking, etc.).
- Cloud Infrastructure (CloudFormation) - *deferred to Phase 1*.
- Real UI implementation.

**Constraints:**
- Pure Kotlin Domain Layer.
- Local-First Validation.
- Strict Version Pinning.

**Unknowns:**
- None significant (Standard Android Setup).

---

## Option A: Infrastructure Baseline (The "Skeleton")

**Summary:** Strictly implements the `implementation_roadmap.md` requirements. Focuses purely on the build system, directory structure, and automation scripts. The application code is minimal (empty classes/Activities).

**Approach:**
- **Modules:** Created but empty (except for `keep` files or bare minimums).
- **DI:** `HiltAndroidApp` and an empty `AppModule`.
- **Validation:** Full suite of scripts (`setup_ci_env.sh`, `run_local_validation.sh`) fully implemented.
- **Dependencies:** Only those required for the skeleton (Hilt, Compose, JUnit).

**Pros:**
- **Lowest Risk:** Exactly matches the "Foundation" definition.
- **Clean:** No temporary "throwaway" code to demonstrate features.
- **Fastest:** Least amount of code to write.

**Cons:**
- **Unverified Runtime Graph:** Does not prove that the Multi-Module DI graph actually works at runtime (only compile time).
- **"Boring":** Nothing visual to show stakeholders.

**High-level file count:** ~30 files (mostly Gradle & Scripts), ~5 dummy Kotlin files.

---

## Option B: The "Tracer Bullet" (Vertical Slice)

**Summary:** Extends Option A by implementing a tiny, read-only "App Version" feature. This forces the creation of a Use Case, a Repository, and a UI connection, verifying the architecture "end-to-end".

**Approach:**
- **Modules:** Fully wired.
- **Domain:** `GetAppVersionUseCase` (Pure Kotlin).
- **Data:** `AppVersionRepository` (Impl in Data).
- **UI:** `DashboardScreen` showing the version string.
- **DI:** fully wired to inject Repo into UseCase into ViewModel.

**Pros:**
- **Verified Architecture:** Proves the strict boundary rules and Hilt wiring work at runtime.
- **Pattern Setter:** Provides a concrete example for future developers to copy.
- **Early Feedback:** Catches visibility/module configuration issues immediately.

**Cons:**
- **Slightly More Effort:** ~4-5 extra files.
- **Temporary Code:** Might need refactoring later (though Version info is a valid requirement).

**High-level file count:** ~35 files.

---

## Option C: The "Heavy" Foundation (Pre-Loaded)

**Summary:** Extends Option A by pre-configuring *all* anticipated libraries (Room, Retrofit, AWS SDK, WorkManager) in the Gradle files and DI modules, even if they aren't used yet.

**Approach:**
- **Dependencies:** Add all libs from `libs.versions.toml` immediately.
- **Boilerplate:** Create `NetworkModule` (AWS Config), `DatabaseModule` (Room DB), `WorkerFactory`.
- **Validation:** Suppress "Unused Dependency" warnings.

**Pros:**
- **"Done is Done":** Validation infrastructure handles the full weight of the app from Day 1.
- **Ready for Features:** Phase 1, 2, 3 can start immediately without plumbing.

**Cons:**
- **YAGNI (You Ain't Gonna Need It):** Adds complexity and build time before it's needed.
- **Noise:** Harder to review the PR due to volume of boilerplate.
- **False Positives:** Validation tools might complain about unused code.

**High-level file count:** ~50 files.

---

## Comparison

| Criteria | Option A (Skeleton) | Option B (Tracer Bullet) | Option C (Heavy) |
|----------|---------------------|--------------------------|------------------|
| **Fit to Goals** | High (Exact match) | High (Adds Verification) | Medium (Over-delivery) |
| **Complexity** | Low | Low-Medium | Medium-High |
| **Risk** | Low | Low | Medium (Integration issues) |
| **Operability** | High | High | High |
| **Verification** | Low (Compile only) | **High (Runtime)** | Medium (Compile only) |
