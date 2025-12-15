# Build & Release Specification

**Related Documents:** [CI Pipeline](../operations/ci_pipeline.md), [Project Structure](project_structure.md)

This document defines the authoritative build process for the Locus Android application. It specifies how source code is transformed into distributable artifacts, ensuring security, reproducibility, and store compliance.

## 1. Build Environment

To ensure consistency between local development and CI environments, strict versioning is enforced.

*   **JDK Version:** **JDK 17 (LTS)** is the mandatory Java runtime for all builds.
*   **Build System:** Gradle (via Wrapper `gradlew`).
*   **Android Gradle Plugin (AGP):** Version defined in `libs.versions.toml`.
*   **Reproducibility:** The build environment must use fixed timestamps and pinned dependencies to support deterministic builds.

## 2. Product Flavors & Artifacts

The project utilizes Gradle Product Flavors to produce distinct artifacts for different distribution channels.

| Flavor | Target Audience | Artifact Type | Dependency Profile |
| :--- | :--- | :--- | :--- |
| **`standard`** | Google Play Store | **AAB** (App Bundle) | Includes proprietary libraries (e.g., Google Play Services, Firebase Crashlytics). |
| **`foss`** | F-Droid / Manual | **APK** (Universal) | Strictly Open Source. No proprietary binaries. Uses "No-Op" stubs for telemetry/location. |

### 2.1. Artifact Configuration
*   **Standard (AAB):** Must be built as an Android App Bundle to allow Google Play to generate optimized APKs (Split APKs) for end-users.
*   **FOSS (APK):** Must be built as a **Universal APK** containing all ABIs (ARM64, x86, etc.) to ensure a single file works on any device without user confusion.

## 3. Versioning Strategy

Versioning is automated to prevent human error and ensure strict monotonicity.

*   **Version Name (Human Readable):** Derived strictly from the current **Git Tag** (e.g., `v1.2.0`).
    *   *Fallback:* If no tag is present (e.g., dev build), use `0.0.0-dev-{short_commit_hash}`.
*   **Version Code (Internal Integer):** Derived from the **Total Commit Count** (`git rev-list --count HEAD`).
    *   *Mechanism:* This ensures that every commit results in a higher version code than the previous one, satisfying upgrade requirements.

## 4. Signing Configuration

Signing keys are managed differently based on the build variant and security context.

### 4.1. Debug Builds
*   **Keystore:** `debug.keystore`
*   **Location:** Committed to the repository.
*   **Purpose:** Allows immediate "checkout and build" for new contributors without configuration.
*   **Security:** Intentionally insecure (Password: `android`).

### 4.2. Release Builds
*   **Keystore:** **NEVER** committed to the repository.
*   **Injection Method:** Credentials must be provided via Environment Variables during the build process.
*   **Required Variables:**
    *   `LOCUS_KEYSTORE_FILE`: Base64 encoded content OR file path to the `.jks` or `.p12` file.
    *   `LOCUS_KEYSTORE_PASSWORD`: Password for the keystore.
    *   `LOCUS_KEY_ALIAS`: Alias of the signing key.
    *   `LOCUS_KEY_PASSWORD`: Password for the specific key.

### 4.3. Key Management Model
*   **Standard (Google Play):** The key used in CI is the **Upload Key**. Google Play re-signs the artifact with the actual App Signing Key before distribution.
*   **FOSS (F-Droid/Manual):** The key used in CI is the **App Signing Key**. This key is the source of truth for identity and must be backed up securely. Loss of this key prevents future updates.
*   **Format:** Keys must use **PKCS12** format.

## 5. Build Configuration & Optimization

### 5.1. Obfuscation (R8)
*   **Enabled:** Yes, for all **Release** builds (`standard` and `foss`).
*   **Configuration:**
    *   `minifyEnabled = true` (Code shrinking & obfuscation)
    *   `shrinkResources = true` (Remove unused resources)
    *   `proguard-rules.pro` must be maintained to prevent stripping of reflection-based components (e.g., Serialization, Room).

### 5.2. Build Config Secrets
*   **Method:** Secrets are injected into `BuildConfig` via Gradle from `local.properties` (Dev) or Environment Variables (CI).
*   **Namespace:** All environment variables must use the `LOCUS_` prefix.
*   **Required Flags:**
    *   `LOCUS_CRASHLYTICS_ENABLED` (Boolean): Controls initialization of crash reporting in the `standard` flavor.

## 6. Dependency Management

*   **FOSS Compliance:** The `foss` flavor must explicitly exclude `com.google.android.gms` and `com.google.firebase` dependencies.
*   **Implementation:**
    *   Use `standardImplementation` in `build.gradle.kts` for proprietary libs.
    *   Use `fossImplementation` for FOSS alternatives or No-Op stubs.
