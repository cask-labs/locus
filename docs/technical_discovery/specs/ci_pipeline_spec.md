# Validation Pipeline (CI/CD)

This document defines the automated verification and delivery pipeline for Locus. The pipeline ensures code quality, security compliance, and build integrity before any code is merged.

## 1. Local-First Philosophy

The validation pipeline is designed to be **Local-First**.
*   **Principle:** "If it fails on CI, it must fail locally."
*   **Implementation:** All CI steps are wrappers around local scripts as defined in the [Automation Scripts Specification](automation_scripts_spec.md).
*   **Determinism:** All tools (linters, scanners) must use **Strict Version Pinning** via lockfiles to ensure the local environment matches CI exactly.
*   **Benefit:** Developers can verify their work fully without pushing to a remote server, supporting the offline/sovereign development model.

## 2. Supply Chain Security

To protect against compromised dependencies and build environments, the following mandates must be enforced in the CI/CD pipeline.

### 2.1. Immutable Actions
*   **Requirement:** All GitHub Actions must be referenced by their full **Commit SHA**, not mutable tags (e.g., `v3`).
*   **Implementation:** `uses: actions/checkout@f43a0e5ff2bd294095638e18286ca9a3d1956744 # v3.6.0`
*   **Automation:** Dependabot must be configured to automatically open Pull Requests to update these hashes.

### 2.2. Least Privilege
*   **Requirement:** All workflows must explicitly declare minimal permissions.
*   **Default:** `permissions: contents: read` must be set at the top level of every workflow to prevent unauthorized write access or token abuse.

### 2.3. AWS Authentication (OIDC)
*   **Requirement:** Use **OpenID Connect (OIDC)** for AWS authentication.
*   **Prohibition:** Long-lived AWS Secret Access Keys are **strictly forbidden** in GitHub Secrets.
*   **Mechanism:** Use `aws-actions/configure-aws-credentials` with an IAM Role Trust Policy that trusts the GitHub OIDC provider.

### 2.4. Script Integrity
*   **Prohibition:** **No Remote Execution**. Commands like `curl ... | bash` are strictly forbidden.
*   **Requirement:** All scripts must be checked into the repository (`scripts/`), reviewed via PR, and linted (`shellcheck`) before execution.

## 3. Tool Versioning Strategy

To ensure reproducible builds, all validation tools must be pinned.

*   **Python Tools:** Managed via `requirements.txt`.
    *   Includes: `cfn-lint`, `checkov`, `boto3`, `semgrep`, `requests`.
    *   **Rule:** Developers must install these via `./scripts/setup_ci_env.sh`.
*   **Gradle Plugins:** Managed via `libs.versions.toml` (Version Catalog).
*   **Shell Utilities:** Checked at runtime by the scripts (e.g., checking `java --version`).

## 4. Developer Environment (Pre-Commit)

To catch issues early and prevent secrets from entering version control, developers must configure local Git hooks.

*   **Tool:** `pre-commit`
*   **Mandatory Hooks:**
    *   **Secret Scanning:** Detects AWS keys, tokens, or private keys (e.g., `git-secrets`, `trufflehog`).
    *   **Syntax Check:** Basic linting for Kotlin, Markdown, and YAML.
    *   **File Size:** Prevents accidental commit of large binaries.

## 5. Validation Tiers

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
    *   **Scope:** Validates syntax and resource references in `docs/technical_discovery/locus-stack.yaml`.
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
*   **Scope:** Real AWS CloudFormation deployment and resource validation.
*   **Tool:** AWS CLI (native CloudFormation commands).
*   **Command:** `./scripts/audit_infrastructure.sh`
*   **Details:** Deploys CloudFormation stack to actual AWS environment, verifies resource creation, validates stack outputs, and automatically cleans up. Requires AWS credentials. See [Advanced Validation Strategy](advanced_validation_spec.md).

### Tier 5: Device Farm & Hardware (Pre-Release)
*   **Scope:** Full end-to-end verification on physical devices.
*   **Trigger:** Manual only (`workflow_dispatch`).
*   **Tool:** AWS Device Farm (via `scripts/run_device_farm.py`).
*   **Details:** See [Advanced Validation Strategy](advanced_validation_spec.md).

## 6. Continuous Integration (GitHub Actions)

The `.github/workflows/validation.yml` workflow orchestrates Tiers 1-3 automatically. Tiers 4 and 5 are triggered manually.

```yaml
name: Validation
on: [push, pull_request]

# Security: Restrict permissions to read-only by default
permissions:
  contents: read

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      # Security: Use SHA pinning for immutable action references
      - uses: actions/checkout@f43a0e5ff2bd294095638e18286ca9a3d1956744 # v3.6.0

      - name: Setup Tools (Deterministic)
        run: ./scripts/setup_ci_env.sh # Installs pinned versions

      - name: Tier 1 - Static Analysis
        # Security: Scripts are local and linted, no curl | bash
        run: ./gradlew ktlintCheck detekt lintDebug && shellcheck scripts/*.sh

      - name: Tier 2 - Tests & Arch
        run: ./gradlew testDebugUnitTest

      - name: Tier 3 - Security Audit
        run: ./scripts/verify_security.sh
```

## 7. Release Automation

We employ a "Best Practice" automated release pipeline to publish to Google Play and GitHub simultaneously.

*   **Trigger:** Pushing a tag (e.g., `v1.0.0`).
*   **Prerequisite:** Release notes must be present in `distribution/fastlane/metadata/android/en-US/changelogs/default.txt`.

### 6.1. Build & Sign
1.  **Build:** Generates both flavors:
    *   `standard`: Android App Bundle (`.aab`) for Google Play.
    *   `foss`: Universal APK (`.apk`) for F-Droid/Sideloading.
2.  **Sign:** Signs artifacts using the Release Keystore injected via secrets.
3.  **SBOM:** Generate a **Software Bill of Materials (CycloneDX)**.

### 6.2. Distribution
*   **Google Play Store (Standard Flavor):**
    *   **Action:** Automatically uploads the Signed AAB and Release Notes.
    *   **Track:** **Internal** (Allows manual promotion to Production after verification).
    *   **Tool:** Gradle Play Publisher (GPP) or GitHub Action.
    *   **Credential:** Uses `LOCUS_PLAY_JSON` Service Account.

*   **GitHub Release (FOSS Flavor):**
    *   **Action:** Creates a GitHub Release.
    *   **Assets:** Attaches the `foss` APK, `standard` AAB, mapping files, and SBOM.
    *   **Notes:** Injects the content from the changelog file.
    *   **F-Droid:** This release serves as the source for F-Droid (which pulls the source or binary) and manual users.
