# Operational Architecture

This directory defines the **Operational Architecture** of the Locus project. It encompasses the systems and processes that ensure the application is built correctly, validated robustly, and operates reliably in the wild.

## Core Philosophy

1.  **Fail-Open Operations:** The application's core functionality (tracking) must **never** be compromised by failures in operational subsystems (telemetry, logging, analytics).
2.  **User Sovereignty:** Operational data (logs, crash reports) belongs to the user first. Centralized aggregation is strictly opt-in.
3.  **Local-First Validation:** All automated checks (CI) must be reproducible on a developer's local machine without proprietary cloud environments.
4.  **Robust Verification:** We employ a tiered validation strategy, ranging from static analysis to on-demand device farm testing.

## Components

The architecture is modularized into the following definitions:

*   **[Validation Pipeline (CI/CD)](ci_pipeline.md):** Defines the automated build, test, and release process (Levels 1-3).
*   **[Advanced Validation Strategy](advanced_validation.md):** Detailed analysis and strategy for Level 4 validation (Device Farms vs. Emulators).
*   **[Runtime Telemetry](telemetry.md):** Specifications for the Dual-Destination telemetry system (User S3 + Opt-in Service) and resilience patterns.
*   **[Runtime Watchdog](runtime_watchdog.md):** Architecture for the internal self-monitoring and self-healing systems.
