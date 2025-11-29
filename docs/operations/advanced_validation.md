# Advanced Validation Strategy (Level 4)

This document details the strategy for **Level 4 Validation**: Full-system integration and UI testing on Android environments. This tier is resource-intensive and is designed to be executed **On-Demand** rather than on every commit.

## 1. Objective

To ensure the application behaves correctly across the fragmented Android ecosystem (different OS versions, screen sizes, and manufacturers) and verifies end-to-end flows (e.g., "Start Tracking" -> "Stop Tracking" -> "Database Persisted").

## 2. Options Analysis

We analyzed two primary approaches for executing Instrumentation Tests (`androidTest`).

| Feature | Option A: GitHub Actions Emulators | Option B: AWS Device Farm |
| :--- | :--- | :--- |
| **Environment** | Virtualized Emulator (x86_64) | Real Hardware (ARM) |
| **Cost** | Free (within Action minutes) | ~$0.17/device minute (Pay-as-you-go) |
| **Speed** | Slow (Booting takes 5-10m) | Fast (Parallel execution) |
| **Reliability** | Low (Flaky, timeouts common) | High |
| **Coverage** | Generic Android Images | Specific OEMs (Samsung, Pixel, etc.) |
| **Recommendation** | **Routine (Nightly)** | **Pre-Release (Golden)** |

## 3. Implementation Strategy

We adopt a **Hybrid Strategy** to balance cost and confidence.

### 3.1. Workflow A: "Smoke Test" (GitHub Actions)
*   **Trigger:** Manual (`workflow_dispatch`) or Nightly Schedule.
*   **Scope:** Runs the core `CriticalPathTests` suite on a single generic API level (e.g., API 33).
*   **Goal:** Catch major regressions in the UI layer without incurring external costs.

### 3.2. Workflow B: "Pre-Release Certification" (AWS Device Farm)
*   **Trigger:** Strictly Manual (`workflow_dispatch` with input "Release Candidate").
*   **Scope:** Runs the full test suite across a defined "Device Pool" (e.g., Top 5 devices by market share).
*   **Mechanism:**
    1.  CI builds `app-debug-androidTest.apk`.
    2.  CI uses `aws devicefarm schedule-run` (using project AWS credentials).
    3.  CI polls for results and downloads the report.
*   **Cost Control:**
    *   This workflow requires the user to configure their own AWS Credentials with `devicefarm:*` permissions.
    *   It is **disabled by default** to prevent accidental billing.

## 4. Test Suites

To support this split, Instrumentation Tests are tagged:
*   `@SmallTest`: Unit-like UI tests.
*   `@SmokeTest`: Critical paths (Start/Stop tracking).
*   `@CompatibilityTest`: Device-specific edge cases (Sensor interaction).

## 5. Deployment Pre-Requisites

For the "Device Farm" strategy to work in a "User-Owned" model:
1.  The user must enable Device Farm in their AWS Console.
2.  The user must add `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` to GitHub Secrets.
3.  The CI script checks for these secrets before attempting execution.
