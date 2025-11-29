# Validation Pipeline (CI/CD)

This document defines the automated verification and delivery pipeline for Locus. The pipeline ensures code quality, security compliance, and build integrity before any code is merged.

## 1. Local-First Philosophy

The validation pipeline is designed to be **Local-First**.
*   **Principle:** "If it fails on CI, it must fail locally."
*   **Implementation:** All CI steps are wrappers around local scripts (e.g., `./gradlew check`, `python3 scripts/run_device_farm.py`).
*   **Benefit:** Developers can verify their work fully without pushing to a remote server, supporting the offline/sovereign development model.

## 2. Validation Tiers

The pipeline executes checks in order of speed and cost.

### Tier 1: Static Analysis & Linting (Fast)
*   **Kotlin:**
    *   **Tool:** `ktlint` (Formatting) and `detekt` (Code Smells).
    *   **Command:** `./gradlew ktlintCheck detekt`
*   **CloudFormation:**
    *   **Tool:** `cfn-lint`
    *   **Scope:** Validates syntax and resource references in `docs/locus-stack.yaml`.
*   **Documentation:**
    *   **Tool:** `markdownlint`
    *   **Scope:** Ensures `docs/` and `agents/` files follow standard Markdown syntax.

### Tier 2: Unit Testing (Fast)
*   **Scope:**
    *   Domain layer, Data layer, ViewModels.
    *   **Simulated Scenarios:** Robolectric tests for Non-Functional requirements (Battery Safety, Network Backoff).
*   **Tool:** JUnit 5, MockK, Robolectric.
*   **Command:** `./gradlew testDebugUnitTest`
*   **Requirement:** 100% Pass rate required.

### Tier 3: Security & Policy (Medium)
*   **Scope:** Infrastructure Security Compliance.
*   **Tool:** `checkov` or `cfn-guard`.
*   **Policies Enforced:**
    *   S3 Buckets must be **Private**.
    *   S3 Buckets must have **Versioning** enabled.
    *   IAM Policies must not use `*` (Wildcards) on sensitive actions.
*   **Command:** `./scripts/verify_security.sh`

### Tier 4: Infrastructure Audit (Optional)
*   **Scope:** Validation of CloudFormation deployment logic (Dry Run).
*   **Tool:** `taskcat` or `aws cloudformation create-change-set`.
*   **Command:** `./scripts/audit_infrastructure.sh`
*   **Details:** Verifies quota limits and circular dependencies without permanent deployment.

### Tier 5: Device Farm & Hardware (Pre-Release)
*   **Scope:** Full end-to-end verification on physical devices.
*   **Trigger:** Manual only (`workflow_dispatch`).
*   **Tool:** AWS Device Farm (via `scripts/run_device_farm.py`).
*   **Details:** See [Advanced Validation Strategy](advanced_validation.md).

## 3. Continuous Integration (GitHub Actions)

The `.github/workflows/validation.yml` workflow orchestrates Tiers 1-3 automatically. Tiers 4 and 5 are triggered manually.

```yaml
name: Validation
on: [push, pull_request]
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Tier 1 - Static Analysis
        run: ./gradlew ktlintCheck detekt && cfn-lint docs/locus-stack.yaml
      - name: Tier 2 - Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Tier 3 - Security Audit
        run: pip install checkov && checkov -f docs/locus-stack.yaml
```

## 4. Release Automation

To support the "User builds from source" model while offering convenience, we employ automated release generation.

*   **Trigger:** Pushing a tag (e.g., `v1.0.0`).
*   **Process:**
    1.  **Build:** `./gradlew assembleRelease`
    2.  **Sign:** Signs the APK using a repository-secret keystore (for official releases) or a debug key (for nightly).
    3.  **Draft:** Creates a GitHub Release draft.
    4.  **Attach:** Uploads `locus-v1.0.0.apk` and `locus-stack.yaml`.
*   **Verification:** The user can compare the checksum of the attached APK with their local build (assuming deterministic builds are enabled).
