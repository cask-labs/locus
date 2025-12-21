# 01-task-1-scaffolding-plan.md

## Prerequisites: Human Action Steps
None. Agent will handle all file movements and creations.

## Implementation Steps

### Step 1: Version Catalog Setup
1.  **Create Directory:** Create `gradle/` directory.
2.  **Move File:** Move `libs.versions.toml` from root to `gradle/libs.versions.toml`.
3.  **Update Content:** Edit `gradle/libs.versions.toml` to include:
    *   **Kover:** `org.jetbrains.kotlinx:kover:0.7.6` (Plugin id: `org.jetbrains.kotlinx.kover`)
    *   **ArchUnit:** `com.tngtech.archunit:archunit-junit5:1.2.1`
    *   **Ensure:** `mockk-android`, `hilt-android`, `hilt-compiler` are present.
    *   **Remove:** Delete the original `libs.versions.toml` from the root.

### Step 2: Root Configuration
1.  **Settings:** Create `settings.gradle.kts` in root.
    *   Configure `pluginManagement` (repositories: google, maven, gradle).
    *   Include modules: `:app`, `:core:data`, `:core:domain`, `:core:testing`.
    *   Load version catalog from `gradle/libs.versions.toml`.
2.  **Root Build:** Create `build.gradle.kts` in root.
    *   Apply `org.jetbrains.kotlinx.kover` (version from catalog).
    *   Register `clean` task (Delete).

### Step 3: Module Scaffolding
Create the following directory structures and build files.

#### A. Domain Layer (`:core:domain`)
*   **Path:** `core/domain/`
*   **Type:** Pure Kotlin Library (`java-library`, `kotlin("jvm")`).
*   **Config:**
    *   Namespace: `com.locus.core.domain`
    *   Java Compatibility: 17
    *   Dependencies: `javax.inject:javax.inject:1` (for Hilt/Dagger interfaces), `kotlinx-coroutines-core`.
    *   **No Android Dependencies.**

#### B. Data Layer (`:core:data`)
*   **Path:** `core/data/`
*   **Type:** Android Library (`com.android.library`, `kotlin("android")`, `com.google.dagger.hilt.android`).
*   **Config:**
    *   Namespace: `com.locus.core.data`
    *   Compile/Target SDK: 34, Min SDK: 28.
    *   Dependencies: `implementation(project(":core:domain"))`, Room, Retrofit, AWS SDK.

#### C. Testing Layer (`:core:testing`)
*   **Path:** `core/testing/`
*   **Type:** Android Library (`com.android.library`).
*   **Config:**
    *   Namespace: `com.locus.core.testing`
    *   Compile/Target SDK: 34, Min SDK: 28.
    *   Dependencies: `implementation(project(":core:domain"))`, JUnit, Mockk.

#### D. App Layer (`:app`)
*   **Path:** `app/`
*   **Type:** Android Application (`com.android.application`, `kotlin("android")`, `com.google.dagger.hilt.android`).
*   **Config:**
    *   Namespace: `com.locus.android`
    *   Compile/Target SDK: 34, Min SDK: 28.
    *   Dependencies:
        *   `implementation(project(":core:domain"))`
        *   `implementation(project(":core:data"))`
        *   `androidTestImplementation(project(":core:testing"))`
        *   Hilt, Compose, WorkManager.

## Spec Alignment

| Component | Spec Behavior |
| :--- | :--- |
| `libs.versions.toml` | **Build Spec 1.1:** Defines dependencies (Kover, ArchUnit). |
| `:core:domain` | **Project Structure 1.2:** Pure Kotlin, no Android deps. |
| `:core:data` | **Project Structure 1.2:** Android Lib, implements Domain. |
| `:app` | **Project Structure 1.2:** Application entry point. |

## Validation Approach

1.  **Module Verification:**
    ```bash
    ./gradlew projects
    ```
    *Expectation:* Lists all 4 modules successfully.

2.  **Build Verification:**
    ```bash
    ./gradlew assembleDebug
    ```
    *Expectation:* BUILD SUCCESSFUL.

## Definition of Done
*   `gradle/libs.versions.toml` exists with Kover/ArchUnit.
*   All 4 modules have valid `build.gradle.kts` files.
*   Project compiles without error.
