# Behavioral Specification: Service Reliability (Watchdog)

**Bounded Context:** This specification governs the "Immune System" of the application, responsible for self-monitoring, detecting crashes or deadlocks, and autonomously recovering the service or alerting the user.

**Prerequisite:** Depends on **[Intelligent Tracking](02_intelligent_tracking.md)** (the system being watched).
**Downstream:** Triggers **[System Status & Feedback](05_system_status_feedback.md)** (for fatal errors).

---

## Passive Heartbeat Monitoring
*   **R7.100** **While** the Tracking Service is in the `RECORDING` state, it **shall** write a timestamp to a lightweight persistence layer every 15 minutes.
*   **R7.200** **When** writing the heartbeat timestamp, the service **shall** use standard local storage (not Encrypted) to ensure high availability and performance.
*   **R7.300** **While** the application is installed, a background process **shall** execute periodically (e.g., every 15 minutes) to monitor health.
*   **R7.400** **When** the monitor executes, it **shall** check the age of the last heartbeat timestamp.
*   **R7.500** **If** the tracking state is `RECORDING` AND the last heartbeat is older than 45 minutes, **then** the monitor **shall** declare the service "Unresponsive" and initiate recovery.

## Autonomous Recovery (Self-Healing)
*   **R7.600** **When** the service is declared Unresponsive, the monitor **shall** attempt to restart the service.
*   **R7.700** **When** attempting a restart on modern OS versions, the monitor **shall** try to launch the visible background service.
*   **R7.800** **If** the restart fails due to OS background start restrictions, **then** the monitor **shall** post a High Priority Notification requiring manual user intervention ("Resume Tracking").

## Circuit Breaker (Anti-Loop)
*   **R7.900** **When** the monitor successfully detects a fresh heartbeat (service running > 15 mins), it **shall** reset the consecutive restart counter to 0.
*   **R7.1000** **When** the monitor attempts a recovery restart, it **shall** increment the consecutive restart counter.
*   **R7.1100** **If** the consecutive restart counter reaches 3, **then** the monitor **shall** "Trip the Circuit Breaker" and cease all further automatic restart attempts.
*   **R7.1200** **When** the Circuit Breaker trips, the system **shall** trigger a Tier 3 Fatal Error notification ("Service Unstable") requiring user action.
*   **R7.1300** **When** the user manually opens the app or toggles tracking, the system **shall** reset the Circuit Breaker.

## Invariant Checks
*   **R7.1400** **When** the monitor executes, it **shall** verify that the background location permission is still granted.
*   **R7.1500** **If** the permission is revoked while `RECORDING`, **then** the monitor **shall** stop the service and trigger a Tier 3 Fatal Error ("Permission Revoked").
*   **R7.1600** **When** the monitor executes, it **shall** check for "Upload Stuck" conditions (Buffer > 4 hours old, Network Connected, Battery > 15%).
*   **R7.1700** **If** an Upload Stuck condition is detected, **then** the monitor **shall** trigger an immediate synchronization attempt.
