# Automation Scripts Specification

**Related Documents:** [CI Pipeline](ci_pipeline_spec.md), [Advanced Validation](advanced_validation_spec.md), [Build Specification](build_spec.md)

This document defines the interface and requirements for the automation scripts used in the Validation Pipeline. These scripts serve as the abstraction layer between the developer/CI system and the underlying tools (Gradle, Python, Bash), ensuring that validation is consistent regardless of where it is executed.

## 1. General Architecture

*   **Language Strategy:**
    *   **Bash (`.sh`):** strictly for "Glue Code" (e.g., wrappers, simple file operations, invoking Gradle).
    *   **Python 3 (`.py`):** strictly for "Logic" (e.g., parsing, complex validation, AWS interactions).
*   **Locality:** Scripts must be executable on a local developer machine (macOS/Linux) without modification.
*   **Idempotency:** Re-running a script must be safe.
*   **Dependencies:** All Python dependencies are defined in `scripts/requirements.txt`.

## 2. Environment Variables

Scripts rely on standard environment variables for configuration. These must be set in the CI environment or a local `.env` file (if supported by the runner).

| Variable | Description | Required By |
| :--- | :--- | :--- |
| `AWS_ACCESS_KEY_ID` | Standard AWS Credentials. | `audit_infrastructure`, `run_device_farm` |
| `AWS_SECRET_ACCESS_KEY` | Standard AWS Credentials. | `audit_infrastructure`, `run_device_farm` |
| `AWS_SESSION_TOKEN` | (Optional) Standard AWS Credentials. | `audit_infrastructure`, `run_device_farm` |
| `AWS_REGION` | Target AWS Region (e.g., `us-east-1`). | `audit_infrastructure`, `run_device_farm` |
| `LOCUS_KEYSTORE_FILE` | Base64 content or path to Release Keystore. | `build_artifacts` (Release) |
| `LOCUS_KEYSTORE_PASSWORD` | Password for the Keystore. | `build_artifacts` (Release) |
| `LOCUS_KEY_ALIAS` | Key Alias. | `build_artifacts` (Release) |
| `LOCUS_KEY_PASSWORD` | Key Password. | `build_artifacts` (Release) |

## 3. Script Inventory

### 3.1. `scripts/setup_ci_env.sh`
**Purpose:** Bootstraps the environment by installing all external tools and pinned dependencies.

*   **Logic:**
    1.  Verify Python 3 is available.
    2.  Install Python dependencies from `scripts/requirements.txt` (e.g., `checkov`, `taskcat`, `boto3`).
    3.  Verify/Install binary tools (e.g., `ktlint` if not managed by Gradle).
*   **Output:** Standard installation logs.

### 3.2. `scripts/run_local_validation.sh` (Tier 2 Wrapper)
**Purpose:** Executes the full suite of "Fast" local tests. This is the "Pre-Flight" check for developers.

*   **Logic:**
    1.  **Static Analysis:** Run Lint (`./gradlew lint`) and ArchUnit tests.
    2.  **Security:** Run `verify_security.sh` (see below).
    3.  **Unit Tests:** Run all Tier 2 tests (`./gradlew testDebugUnitTest`).
*   **Output:** Pass/Fail status. Fails if *any* check fails.

### 3.3. `scripts/build_artifacts.sh`
**Purpose:** Wraps the Gradle build commands to generate distributable artifacts.

*   **Inputs:**
    *   `--flavor`: `standard` or `foss`.
    *   `--build-type`: `debug` or `release`.
*   **Logic:**
    1.  Map inputs to Gradle tasks (e.g., `assembleStandardRelease`).
    2.  Execute the Gradle wrapper.
    3.  Move/Copy artifacts to a standard output directory (e.g., `dist/`).
*   **Output:** Path to the generated AAB or APK.

### 3.4. `scripts/verify_security.sh`
**Purpose:** Runs security scanners on the codebase.

*   **Logic:**
    1.  **Secret Scanning:** Run `trufflehog` (filesystem scan).
    2.  **IaC Scanning:** Run `checkov` against `docs/locus-stack.yaml`.
    3.  **SAST:** Run `semgrep` (using local rules or standard registry).
*   **Output:** Aggregate report. Fail on High/Critical severities.

### 3.5. `scripts/audit_infrastructure.sh` (Tier 4)
**Purpose:** Validates CloudFormation templates using `taskcat`.

*   **Prerequisite:** AWS Credentials must be active.
*   **Logic:**
    1.  Generate a temporary `_taskcat_override.yml` with a randomized Stack Name (e.g., `locus-audit-$RANDOM`).
    2.  Execute `taskcat test run`.
    3.  Ensure stack deletion (handled by Taskcat, verified by script).
*   **Output:** Taskcat Report.

### 3.6. `scripts/run_device_farm.py` (Tier 5)
**Purpose:** Orchestrates the upload and execution of tests on AWS Device Farm.

*   **Inputs:**
    *   `--app-path`: Path to app APK (Default: `app/build/outputs/apk/debug/app-debug.apk`).
    *   `--test-path`: Path to test APK (Default: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`).
    *   `--project-arn`: Device Farm Project ARN.
    *   `--device-pool-arn`: Device Farm Device Pool ARN.
*   **Logic:**
    1.  **Upload:** `boto3` upload of both APKs.
    2.  **Schedule:** Create a run with the specified configuration.
    3.  **Monitor:** Poll status every 30s (Max 30m).
    4.  **Artifacts:** Download XML reports and Screenshots to `test_results/`.
*   **Output:** Exit Code 0 for Pass, 1 for Fail/Timeout.

## 4. Dependency Definition (`scripts/requirements.txt`)

This file acts as the lockfile for all Python-based automation tools.

```text
# Infrastructure Validation
taskcat>=0.9.0
cfn-lint>=0.86.0
checkov>=3.0.0

# AWS Interaction
boto3>=1.34.0

# Security
semgrep>=1.50.0
# trufflehog is typically a binary, but if a python wrapper exists, list it here.
```
