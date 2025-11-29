# Validation Pipeline (CI/CD)

This document defines the automated verification and delivery pipeline for Locus. The pipeline ensures code quality, security compliance, and build integrity before any code is merged.

## 1. Local-First Philosophy

The validation pipeline is designed to be **Local-First**.
*   **Principle:** "If it fails on CI, it must fail locally."
*   **Implementation:** All CI steps are wrappers around local scripts (e.g., `./gradlew check`, `./scripts/audit_infra.sh`).
*   **Determinism:** All tools (linters, scanners) must use **Strict Version Pinning** via lockfiles (e.g., `requirements.txt`, `Gemfile`, Docker) to ensure the local environment matches CI exactly.
*   **Benefit:** Developers can verify their work fully without pushing to a remote server, supporting the offline/sovereign development model.

## 2. Developer Environment (Pre-Commit)

To catch issues early and prevent secrets from entering version control, developers must configure local Git hooks.

*   **Tool:** `pre-commit`
*   **Mandatory Hooks:**
    *   **Secret Scanning:** Detects AWS keys, tokens, or private keys (e.g., `git-secrets`, `trufflehog`).
    *   **Syntax Check:** Basic linting for Kotlin, Markdown, and YAML.
    *   **File Size:** Prevents accidental commit of large binaries.

## 3. Validation Tiers

The pipeline executes checks in order of speed and cost.

### Tier 1: Static Analysis & Linting (Fast)
*   **Kotlin / Android:**
    *   **Tools:** `ktlint` (Formatting), `detekt` (Code Smells), `Android Lint` (Framework issues).
    *   **Command:** `./gradlew ktlintCheck detekt lintDebug`
*   **Shell Scripts:**
    *   **Tool:** `ShellCheck`
    *   **Scope:** Validates all scripts in `scripts/` and hooks.
*   **CloudFormation:**
    *   **Tool:** `cfn-lint` (Pinned Version)
    *   **Scope:** Validates syntax and resource references in `docs/locus-stack.yaml`.
*   **Legal:**
    *   **Tool:** License Compliance Check (e.g., `FOSSology` or Gradle License Plugin).
    *   **Scope:** Ensures no GPL/Proprietary libraries are linked.

### Tier 2: Unit Testing & Architecture (Fast)
*   **Functional Logic:**
    *   **Scope:** Domain layer, Data layer (Room DAOs via Robolectric), and ViewModels.
    *   **Tool:** JUnit 5, MockK.
    *   **Command:** `./gradlew testDebugUnitTest`
*   **Architecture Governance:**
    *   **Tool:** `ArchUnit`
    *   **Scope:** Enforces rules defined in `agents/rules/android_architecture.md` (e.g., "Domain layer must not depend on Android SDK").
*   **Privacy Regression:**
    *   **Scope:** Telemetry & Data Transmission.
    *   **Check:** Verify that `CommunityUploadWorker` *never* executes if the user setting `opt_in_community` is `false`.

### Tier 3: Security & Policy (Medium)
*   **Infrastructure Security:**
    *   **Tool:** `checkov` or `cfn-guard` (Pinned Version).
    *   **Policies Enforced:**
        *   S3 Buckets must be **Private**.
        *   S3 Buckets must have **Versioning** enabled.
        *   IAM Policies must not use `*` (Wildcards) on sensitive actions.
*   **Application Security (SAST):**
    *   **Tool:** CodeQL or MobSF.
    *   **Scope:** Scans Kotlin code for SQL Injection, Insecure Intents, or Unsafe Reflection.
*   **Command:** `./scripts/verify_security.sh`

## 4. Continuous Integration (GitHub Actions)

The `.github/workflows/validation.yml` workflow orchestrates these tiers.

```yaml
name: Validation
on: [push, pull_request]
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Tools (Deterministic)
        run: ./scripts/setup_ci_env.sh # Installs pinned versions
      - name: Tier 1 - Static Analysis
        run: ./gradlew ktlintCheck detekt lintDebug && shellcheck scripts/*.sh
      - name: Tier 2 - Tests & Arch
        run: ./gradlew testDebugUnitTest
      - name: Tier 3 - Security Audit
        run: ./scripts/verify_security.sh
```

## 5. Release Automation

To support the "User builds from source" model while offering convenience, we employ automated release generation.

*   **Trigger:** Pushing a tag (e.g., `v1.0.0`).
*   **Process:**
    1.  **Build:** `./gradlew assembleRelease`
    2.  **Determinism:** Verify build reproducibility (checksum match against clean runner).
    3.  **SBOM:** Generate a **Software Bill of Materials (CycloneDX)** listing all dependencies.
    4.  **Sign:** Signs the APK using the Project Keystore.
    5.  **Draft:** Creates a GitHub Release draft.
    6.  **Attach:** Uploads `locus-v1.0.0.apk`, `locus-stack.yaml`, and `sbom.json`.
*   **Verification:** The user can compare the checksum of the attached APK with their local build.
