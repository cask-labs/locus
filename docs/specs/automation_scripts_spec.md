# Automation Scripts Specification

**Related Documents:** [CI Pipeline](ci_pipeline_spec.md), [Advanced Validation](advanced_validation_spec.md)

This document defines the requirements for the automation scripts used in the Validation Pipeline. These scripts serve as the "Interface" between the developer/CI system and the underlying tools.

## 1. General Requirements

*   **Language Strategy:** The project enforces a strict "Right Tool for the Job" split:
    *   **Bash (`.sh`):** Strictly for "Glue Code" (e.g., CI wrappers, simple file operations, invoking Gradle/Git).
    *   **Python 3 (`.py`):** Strictly for "Logic" (e.g., parsing JSON/XML, complex validation, AWS `boto3` interactions).
*   **Locality:** Scripts must be executable on a local developer machine (macOS/Linux) without modification.
*   **Idempotency:** Re-running a script should be safe and deterministic.
*   **Exit Codes:** Scripts must return `0` for Success and non-zero for Failure.
*   **Dependencies:** Scripts must check for required tools (e.g., `checkov`, `aws`) and fail with a clear message if they are missing.

## 2. Script Definitions

### 2.1. `scripts/setup_ci_env.sh`
**Purpose:** Bootstraps the environment by installing all pinned dependencies.

*   **Inputs:** None (Reads `requirements.txt`, `Gemfile`, etc.).
*   **Logic:**
    1.  Check for Python 3.
    2.  Install Python dependencies from `requirements.txt` (e.g., `checkov`, `cfn-lint`, `taskcat`).
    3.  Install specific binary versions if not managed by package managers (e.g., specific `ktlint` version).
*   **Output:** Standard installation logs.

### 2.2. `scripts/audit_infrastructure.sh`
**Purpose:** Validates CloudFormation templates using `taskcat`.

*   **Inputs:** AWS Credentials (via Environment Variables).
*   **Logic:**
    1.  Verify AWS credentials exist.
    2.  **Generate Config:** Create a temporary `_taskcat_override.yml` on-the-fly. This allows injecting a randomized stack name (e.g., `locus-audit-$RANDOM`) to ensure isolation.
    3.  **Run:** Execute `taskcat test run` using the generated config.
    4.  **Cleanup:** Must strictly ensure the stack is deleted, even if the test fails (Taskcat handles this, but verify configuration).
*   **Output:** Pass/Fail status based on stack creation success.

### 2.3. `scripts/verify_security.sh`
**Purpose:** Runs security scanners on the codebase.

*   **Inputs:** Codebase path (defaults to current directory).
*   **Logic:**
    1.  **Secret Scanning:** Run `trufflehog` (or equivalent) to check for committed keys.
    2.  **IaC Scanning:** Run `checkov` against `docs/locus-stack.yaml`.
        *   *Config:* Must suppress "Encryption" rules for S3 if they conflict with the "User-Owned" requirement (though they shouldn't).
    3.  **SAST:** Run `semgrep` or `CodeQL` (if available locally) for Kotlin security checks.
*   **Output:** Aggregate report of vulnerabilities. Fail if any "High" or "Critical" issues are found.

### 2.4. `scripts/run_device_farm.py`
**Purpose:** Orchestrates the upload and execution of tests on AWS Device Farm.

*   **Inputs:**
    *   `--app-path`: Path to the `.apk`.
    *   `--test-path`: Path to the instrumentation test APK.
    *   `--project-arn`: AWS Device Farm Project ARN.
    *   `--device-pool-arn`: AWS Device Farm Device Pool ARN.
*   **Logic:**
    1.  **Upload:** Use `boto3` to upload both APKs to Device Farm.
    2.  **Wait:** Poll `get_upload` until status is `SUCCEEDED`.
    3.  **Schedule:** Call `schedule_run` with the specific configuration.
    4.  **Monitor:** Poll `get_run` every 30 seconds.
        *   *Timeout:* Fail if run takes > 30 minutes.
    5.  **Artifacts:** On completion, list and download the XML test reports and screenshots.
*   **Output:** Final Run Result (`PASSED`, `FAILED`, `ERRORED`) and path to directory containing downloaded artifacts (for downstream parsing).

## 3. Tool Versioning Strategy

To ensure determinism, the project relies on a `requirements.txt` file at the root (or in `scripts/`) to pin Python-based tools.

**Required Pins (Baseline):**
*   `cfn-lint >= 0.86.0`
*   `checkov >= 3.0.0`
*   `taskcat >= 0.9.0`
*   `boto3 >= 1.34.0`

*Note: Developers should freeze specific versions (e.g., `3.0.12`) when initializing the project.*
