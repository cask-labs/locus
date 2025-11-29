# Advanced Validation Strategy

This document details the strategy for **Advanced Validation** (Tiers 4 & 5). This includes full-system integration, hardware verification, and infrastructure validation. These tiers are resource-intensive or require external credentials, so they are designed to be executed **On-Demand** rather than on every commit.

## 1. Objective

To ensure the application behaves correctly across the fragmented Android ecosystem (different OS versions, screen sizes, and manufacturers), complies with non-functional constraints (Battery, Network), and correctly interfaces with AWS infrastructure.

## 2. Tier 4: Infrastructure Audit (Optional)

This tier validates that the Infrastructure-as-Code (CloudFormation) is not only syntactically correct but also deployable within the constraints of the AWS environment.

*   **Trigger:** Manual (`workflow_dispatch`) or Local Execution.
*   **Philosophy:** **Local-First.** The logic must be encapsulated in a script (e.g., `scripts/audit_infrastructure.sh`) that can run on a developer's machine given valid AWS credentials.
*   **Mechanism:**
    *   **Tool:** `taskcat` (AWS CloudFormation testing tool) or `aws cloudformation deploy --dry-run` / `create-change-set`.
    *   **Scope:** Attempts to stage the `locus-stack.yaml` in a temporary test stack (e.g., `locus-test-<uuid>`).
    *   **Verification:** Checks for circular dependencies, quota limits, and valid property configurations that `cfn-lint` might miss.
    *   **Cleanup:** Automatically deletes the test stack after verification to prevent cost accumulation.

## 3. Tier 5: Device Farm & Hardware (Pre-Release)

We adopt a **Hybrid Strategy** to balance cost and confidence, separating "Simulated" logic validation from "Physical" hardware validation.

### 3.1. Non-Functional Validation (Simulated Scenarios)
Before paying for real devices, we validate critical battery and network logic using fast, local simulations.

*   **Scope:** Battery Safety Protocol, Network Backoff, Data Buffering.
*   **Tool:** **Robolectric** (Unit Tests).
*   **Method:**
    *   Mock system broadcasts (e.g., `Intent.ACTION_BATTERY_LOW`).
    *   Mock connectivity states (e.g., `ConnectivityManager`).
    *   **Assert:** Verify that `TrackerService` transitions states (e.g., "Active" -> "Power Saving") or `SyncWorker` pauses uploads without requiring a real emulator.
*   **Benefit:** Catches 90% of logic errors instantly and for free.

### 3.2. Hardware Verification (AWS Device Farm)
Real-world validation on physical devices to catch OEM-specific aggression (e.g., Samsung killing background services) or hardware sensor quirks.

*   **Trigger:** **Strictly Manual** (`workflow_dispatch` input: "Release Candidate").
*   **Philosophy:** **Human-Initiated, Machine-Executed.** Once a human approves the cost, the entire process is fully automated.
*   **Orchestration:**
    *   **Script:** A custom Python script (`scripts/run_device_farm.py`) orchestrates the workflow.
    *   **Portability:** This script must be runnable from both GitHub Actions (CI) and a local developer terminal (provided AWS credentials are set).
*   **Workflow:**
    1.  **Build:** Generate `app-debug-androidTest.apk` and `app-debug.apk`.
    2.  **Upload:** The script uploads binaries to AWS Device Farm.
    3.  **Schedule:** The script schedules a run against a defined "Device Pool" (e.g., Top 5 Android devices by market share).
    4.  **Poll:** The script actively polls the run status.
    5.  **Download:** On completion, it downloads the XML test results and screenshots.
*   **Cost Control:**
    *   Disabled by default.
    *   Requires explicit user `devicefarm:*` permissions.
    *   Estimated cost ~$0.17/device-minute.

## 4. Reporting & Feedback

Raw logs from remote systems are difficult to parse. The validation pipeline must provide immediate, actionable feedback.

*   **Mechanism:** The CI pipeline (or local script) must parse standard JUnit XML reports.
*   **Output:**
    *   **GitHub Job Summary:** A Markdown table displayed on the Action run page summarizing Pass/Fail/Skip counts.
    *   **Pull Request Comment:** If triggered on a PR, the bot posts a sticky comment with the results.
*   **Artifacts:** Full logs and screenshots are zipped and attached as Build Artifacts for debugging failures.

## 5. Test Suites

Instrumentation Tests are tagged to support selective execution:

*   `@SmallTest`: Unit-like UI tests (Robolectric or Emulated).
*   `@SimulatedScenario`: Non-functional logic tests (Robolectric).
*   `@SmokeTest`: Critical paths (Start -> Stop -> Verify Persistence).
*   `@CompatibilityTest`: Device-specific edge cases (Sensor interaction, OEM background restrictions).
