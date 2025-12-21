# Deep Dive: Automation Scripts (Validation Pipeline)

## Context

The "Validation Pipeline" is a key component of the Tracer Bullet architecture. To ensure consistency between local development and CI, all checks are wrapped in Bash scripts. This document defines the exact contracts (inputs, outputs, and requirements) for these scripts.

## 1. `scripts/setup_ci_env.sh`

**Purpose:** Bootstraps the environment by installing necessary tools (mostly Python dependencies). It assumes Java and Android SDK are already present (standard for CI runners and Dev machines).

**Inputs:**
- `scripts/requirements.txt` (Python deps)

**Outputs:**
- Exit Code: 0 (Success), Non-zero (Failure)
- Logs: Installation progress

**Logic:**
```bash
#!/bin/bash
set -e

# check if python3 exists
if ! command -v python3 &> /dev/null; then
    echo "Python 3 is required."
    exit 1
fi

# Create venv if not exists
if [ ! -d "venv" ]; then
    python3 -m venv venv
fi

# Activate venv
source venv/bin/activate

# Install deps
pip install -r scripts/requirements.txt

echo "Environment setup complete."
```

## 2. `scripts/requirements.txt`

**Purpose:** Defines Python dependencies for automation.

**Content:**
```text
boto3==1.34.0    # AWS SDK
taskcat==0.9.53  # CloudFormation testing (Future use)
checkov==3.1.0   # Security scanning (Future use)
```

## 3. `scripts/run_local_validation.sh`

**Purpose:** The primary "Gatekeeper" script. Runs all Tier 1 (Static) and Tier 2 (Unit) checks. This is what the Developer runs before pushing, and what CI runs on PRs.

**Inputs:**
- None (Runs on current repo state)

**Outputs:**
- Exit Code: 0 (All pass), Non-zero (Any fail)
- Artifacts: `app/build/reports/lint-results.xml`, `**/build/test-results/**/*.xml`

**Logic:**
```bash
#!/bin/bash
set -e

echo "Running Lint..."
./gradlew lintDebug

echo "Running Unit Tests..."
./gradlew testDebugUnitTest

# Future: Add Detekt or other static analysis here

echo "Validation Successful!"
```

## 4. `scripts/build_artifacts.sh`

**Purpose:** Generates the distributable binaries.

**Inputs:**
- Environment Variables for Signing (Optional for Debug, Required for Release)

**Outputs:**
- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/foss/release/app-foss-release-unsigned.apk` (or signed if env vars present)

**Logic:**
```bash
#!/bin/bash
set -e

BUILD_TYPE=$1 # "debug" or "release"

if [ "$BUILD_TYPE" == "release" ]; then
    echo "Building Release..."
    ./gradlew bundleStandardRelease assembleFossRelease
else
    echo "Building Debug..."
    ./gradlew bundleStandardDebug assembleFossDebug
fi
```

## Decisions

1.  **Wrapper Strategy:** We use simple Bash wrappers around Gradle commands to ensure commands are identical in CI and Local.
2.  **Python for Logic:** If logic gets complex (e.g., parsing results), we move it to Python scripts in `scripts/` called by the Bash wrappers. For Phase 0, Bash is sufficient.
3.  **Strict Failure:** All scripts use `set -e` to fail fast.
