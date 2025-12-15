# Validation Pipeline (CI/CD)

This document defines the automated verification and delivery pipeline for Locus. The pipeline ensures code quality, security compliance, and build integrity before any code is merged.

## 1. Local-First Philosophy

The validation pipeline is designed to be **Local-First**.
*   **Principle:** "If it fails on CI, it must fail locally."
*   **Implementation:** All CI steps are wrappers around local scripts as defined in the [Automation Scripts Specification](automation_scripts_spec.md).
*   **Determinism:** All tools (linters, scanners) must use **Strict Version Pinning** via lockfiles to ensure the local environment matches CI exactly.
*   **Benefit:** Developers can verify their work fully without pushing to a remote server, supporting the offline/sovereign development model.

## 2. Tool Versioning Strategy

To ensure reproducible builds, all validation tools must be pinned.

*   **Python Tools:** Managed via `requirements.txt`.
    *   Includes: `cfn-lint`, `checkov`, `taskcat`, `boto3`.
    *   **Rule:** Developers must install these via `./scripts/setup_ci_env.sh`.
*   **Gradle Plugins:** Managed via `libs.versions.toml` (Version Catalog).
*   **Shell Utilities:** Checked at runtime by the scripts (e.g., checking `java --version`).

## 3. Developer Environment (Pre-Commit)

To catch issues early and prevent secrets from entering version control, developers must configure local Git hooks.

*   **Tool:** `pre-commit`
*   **Mandatory Hooks:**
    *   **Secret Scanning:** Detects AWS keys, tokens, or private keys (e.g., `git-secrets`, `trufflehog`).
    *   **Syntax Check:** Basic linting for Kotlin, Markdown, and YAML.
    *   **File Size:** Prevents accidental commit of large binaries.

## 4. Validation Tiers

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
    *   **Tool:** JUnit 5, MockK, Robolectric.
    *   **Command:** `./gradlew testDebugUnitTest`
    *   **Simulated Scenarios:** Robolectric tests for Non-Functional requirements (Battery Safety, Network Backoff).
*   **Architecture Governance:**
    *   **Tool:** `ArchUnit`
    *   **Scope:** Enforces rules defined in `agents/rules/android_architecture.md` and detailed in the [Testing Specification](testing_spec.md#9-architecture-validation-archunit).
*   **Privacy Regression:**
    *   **Scope:** Telemetry & Data Transmission.
    *   **Check:** Verify that `CommunityUploadWorker` *never* executes if the user setting `opt_in_community` is `false`.

### Tier 3: Security & Policy (Medium)
*   **Infrastructure Security:**
    *   **Tool:** `checkov` (Pinned Version).
    *   **Policies Enforced:**
        *   S3 Buckets must be **Private**.
        *   S3 Buckets must have **Versioning** enabled.
        *   IAM Policies must not use `*` (Wildcards) on sensitive actions.
*   **Application Security (SAST):**
    *   **Tool:** CodeQL or MobSF.
    *   **Scope:** Scans Kotlin code for SQL Injection, Insecure Intents, or Unsafe Reflection.
*   **Command:** `./scripts/verify_security.sh`

### Tier 4: Infrastructure Audit (Manual Execution)
*   **Scope:** Validation of CloudFormation deployment logic (Dry Run).
*   **Tool:** `taskcat`.
*   **Command:** `./scripts/audit_infrastructure.sh`
*   **Details:** Verifies quota limits and circular dependencies without permanent deployment. Requires AWS credentials. See [Advanced Validation Strategy](advanced_validation_spec.md).

### Tier 5: Device Farm & Hardware (Pre-Release)
*   **Scope:** Full end-to-end verification on physical devices.
*   **Trigger:** Manual only (`workflow_dispatch`).
*   **Tool:** AWS Device Farm (via `scripts/run_device_farm.py`).
*   **Details:** See [Advanced Validation Strategy](advanced_validation_spec.md).

## 5. Continuous Integration (GitHub Actions)

The `.github/workflows/validation.yml` workflow orchestrates Tiers 1-3 automatically. Tiers 4 and 5 are triggered manually.

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

## 6. Release Automation

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
