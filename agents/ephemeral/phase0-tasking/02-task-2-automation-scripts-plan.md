# Implementation Plan - Task 2: Automation Scripts

## 1. Prerequisites (Human Action Steps)
*None. This is a net-new implementation.*

## 2. Implementation Steps

### Step 1: Define Dependencies
**Goal:** Establish the version-locked dependencies for all Python-based tools and the base configuration for Taskcat.
*   **Create `scripts/requirements.txt`:**
    *   Add `taskcat>=0.9.0`
    *   Add `cfn-lint>=0.86.0`
    *   Add `checkov>=3.0.0`
    *   Add `boto3>=1.34.0`
    *   Add `semgrep>=1.50.0`
*   **Create `.taskcat.yml`:**
    *   Define project root configuration.
    *   Set regions (e.g., `us-east-1`).
    *   Define template path (pointing to `docs/technical_discovery/locus-stack.yaml`).

### Step 2: Environment Bootstrap Script
**Goal:** Create the script that sets up the CI/Local environment.
*   **Create `scripts/setup_ci_env.sh`:**
    *   Check for Python 3 (`python3 --version`).
    *   Install pip requirements (`pip install -r scripts/requirements.txt`).
    *   Verify `trufflehog` is present (fail with install instructions if missing).
    *   Verify `java` is present.

### Step 3: Local Validation Suite
**Goal:** Implement the "Fast" feedback loop scripts.
*   **Create `scripts/verify_security.sh`:**
    *   Run `trufflehog filesystem .` (exclude `.git`).
    *   Run `checkov -f docs/technical_discovery/locus-stack.yaml`.
    *   Run `semgrep scan --config=p/default`.
*   **Create `scripts/run_local_validation.sh`:**
    *   Run `./gradlew lintDebug`.
    *   Run `./gradlew testDebugUnitTest`.
    *   Run `./gradlew ktlintCheck`.
    *   Run `./scripts/verify_security.sh`.

### Step 4: Build & Infrastructure Scripts
**Goal:** Implement the build wrappers and infrastructure audit tools.
*   **Create `scripts/build_artifacts.sh`:**
    *   Accept `--flavor` and `--build-type` arguments.
    *   Map to Gradle tasks (e.g., `assembleStandardRelease`).
    *   Move output to `dist/`.
*   **Create `scripts/audit_infrastructure.sh`:**
    *   Generate `_taskcat_override.yml` with a random `StackName`.
    *   Run `taskcat test run`.
    *   Cleanup override file.
*   **Create `scripts/run_device_farm.py`:**
    *   Implement Python logic using `boto3`.
    *   Upload APKs (app & test).
    *   Schedule run.
    *   Poll status.
    *   Download artifacts.

### Step 5: CI Integration
**Goal:** Orchestrate the scripts in GitHub Actions.
*   **Create `.github/workflows/validation.yml`:**
    *   Define `on: [push, pull_request]`.
    *   Job `validate`:
        *   Checkout (SHA pinned).
        *   Run `scripts/setup_ci_env.sh`.
        *   Run `scripts/run_local_validation.sh`.

### Step 6: Finalize Permissions
**Goal:** Ensure scripts are executable.
*   **Run:** `chmod +x scripts/*.sh`

## 3. Spec Alignment
*   **`setup_ci_env.sh`** -> `automation_scripts_spec.md` (Section 3.1)
*   **`run_local_validation.sh`** -> `automation_scripts_spec.md` (Section 3.2), `ci_pipeline_spec.md` (Tier 1 & 2)
*   **`build_artifacts.sh`** -> `automation_scripts_spec.md` (Section 3.3)
*   **`verify_security.sh`** -> `automation_scripts_spec.md` (Section 3.4), `ci_pipeline_spec.md` (Tier 3)
*   **`audit_infrastructure.sh`** -> `automation_scripts_spec.md` (Section 3.5), `ci_pipeline_spec.md` (Tier 4)
*   **`run_device_farm.py`** -> `automation_scripts_spec.md` (Section 3.6), `ci_pipeline_spec.md` (Tier 5)

## 4. Validation
*   **Manual Verify:** Run `./scripts/setup_ci_env.sh` to confirm dependencies install.
*   **Manual Verify:** Run `./scripts/run_local_validation.sh` (Note: Gradle steps will fail until Task 1 is done, but the script logic can be verified).
