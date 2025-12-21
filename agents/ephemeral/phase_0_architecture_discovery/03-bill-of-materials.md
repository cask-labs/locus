# Bill of Materials: Phase 0 (Foundation)

## Architecture
**Option B (Tracer Bullet):** Infrastructure Skeleton + "App Version" Vertical Slice.

## Edges

**Inbound:**
- **User Interaction:** Launcher Icon -> `MainActivity`.
- **Validation:** CLI execution of `scripts/*.sh`.

**Outbound:**
- **Build Artifacts:** `app-release.aab` (Standard), `app-release.apk` (FOSS).
- **Test Reports:** JUnit XML, Lint Reports.

## Feature Roots

- **`:app`**: Android App (UI, DI Root).
- **`:core:domain`**: Pure Kotlin Library (Business Logic).
- **`:core:data`**: Android Library (Repositories).
- **`:core:testing`**: Test Fixtures/Helpers.

## Cross-Cutting Themes

**Dependency Injection:**
- **Hilt (Dagger):** Used for Application-wide DI.
- **Strategy:** `SingletonComponent` for Repositories, `ViewModelComponent` for UI.

**Validation Strategy (Local-First):**
- **Wrapper Scripts:** All CI actions must be runnable locally via `scripts/`.
- **Strict Pinning:** All tools (Java, Gradle, Python libs) must be versioned.

**Versioning:**
- **Semantic Versioning:** `vX.Y.Z` derived from Git Tags.
- **Version Code:** `git rev-list --count HEAD` (Total Commit Count).

**Build Variants:**
- **Standard:** Includes `play-services-location` (even if unused yet), `firebase-crashlytics` (stubbed/disabled initially).
- **FOSS:** Strict exclusion of proprietary libs.

## Configuration (Project Level)

**`libs.versions.toml` (Version Catalog):**
- `kotlin`: `1.9.22`
- `agp` (Android Gradle Plugin): `8.2.0`
- `hilt`: `2.50`
- `compose-compiler`: `1.5.8` (matches Kotlin 1.9.22)
- `coroutines`: `1.7.3`
- `room`: `2.6.1` (Added to catalog, but implementation deferred if not needed for Tracer Bullet - *Decision: Defer Room implementation to Phase 2, strictly stick to Tracer Bullet scope*).

**`gradle.properties`:**
- `kotlin.code.style=official`
- `android.useAndroidX=true`

**Environment Variables (`.env` / CI Secrets):**
- `LOCUS_KEYSTORE_FILE` (Release)
- `LOCUS_KEYSTORE_PASSWORD` (Release)
- `LOCUS_KEY_ALIAS` (Release)
- `LOCUS_KEY_PASSWORD` (Release)

## Dependencies (New)

| Library | Module | Purpose |
| :--- | :--- | :--- |
| `androidx.core:core-ktx` | `:app`, `:core:data` | Kotlin Extensions |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `:app` | Lifecycle-aware components |
| `androidx.activity:activity-compose` | `:app` | Compose Host |
| `androidx.compose.ui:ui` | `:app` | UI Framework |
| `androidx.compose.material3:material3` | `:app` | Design System |
| `com.google.dagger:hilt-android` | `:app`, `:core:data` | Dependency Injection |
| `javax.inject:javax.inject` | `:core:domain` | JSR-330 Annotations (Pure Java/Kotlin) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `:core:domain` | Concurrency |
| `junit:junit` | * | Unit Testing |
| `io.mockk:mockk` | * | Mocking |
| `com.google.truth:truth` | * | Assertions |

## Automation Scripts (Inventory)

- `scripts/setup_ci_env.sh`: Bootstraps Python & Tools.
- `scripts/run_local_validation.sh`: Runs Lint, Detekt, Tests.
- `scripts/build_artifacts.sh`: Builds AAB/APK.
- `scripts/verify_security.sh`: Runs Checkov/Trufflehog (stubs/placeholders initially if tools missing).
- `scripts/requirements.txt`: Python dependencies (`boto3`, `taskcat`).

## Open Questions
- None.
