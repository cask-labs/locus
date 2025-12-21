# Behavioral Specification: System Status & Feedback

**Bounded Context:** This specification governs how the internal states of the system (Tracking, Sync, Error) are aggregated and communicated to the user via Notifications, Dashboard elements, and Dialogs.

**Prerequisite:** Depends on **[All Other Contexts]** (as it displays their state).

---

## Persistent Notification
*   **R5.100** **While** the tracking service is active, the system **shall** display a Persistent Notification (visible background process indicator) to prevent OS termination.
*   **R5.200** **When** updating the notification, the system **shall** strictly adhere to the format: `[Recording Status] • [Sync Status]`.
*   **R5.300** **When** the system state changes (e.g., Location acquired, Sync complete, Low Battery), the system **shall** immediately update the notification text.
*   **R5.400** **If** a transient error occurs (Tier 1) or an environmental pause (Tier 2), **then** the system **shall** update the notification text (e.g., "Waiting for Network") without triggering sound or vibration.

## Dashboard Status
*   **R5.500** **When** viewing the Dashboard, the system **shall** display the current "Local Buffer" count (data points waiting on device).
*   **R5.600** **When** viewing the Dashboard, the system **shall** display the "Last Sync" time (relative timestamp).
*   **R5.700** **When** the local buffer count exceeds 10,000 points, the system **shall** display a "Buffer Warning" indicator.
*   **R5.800** **When** the "Stop Tracking" action is triggered, the system **shall** display a confirmation dialog to prevent accidental data gaps.

## Error Handling Hierarchy
*   **R5.900** **If** a **Tier 1 (Transient)** error occurs (e.g., Network Timeout, Server Error), **then** the system **shall** handle it silently via exponential backoff and **shall not** notify the user.
*   **R5.1000** **If** a **Tier 2 (Environmental)** condition occurs (e.g., Low Battery, Airplane Mode), **then** the system **shall** update the Persistent Notification text but **shall not** play a sound or vibrate.
*   **R5.1100** **If** a **Tier 3 (Fatal)** error occurs (e.g., Authentication Failure, Permission Revoked, Service Crash Loop), **then** the system **shall** trigger a High Priority Notification with sound and vibration.
*   **R5.1200** **When** a Tier 3 error occurs, the system **shall** display a Blocking Full-Screen Error overlay that prevents interaction until the issue is resolved.

## Manual Sync Feedback
*   **R5.1300** **When** a Manual Sync is initiated, the system **shall** display an indeterminate progress indicator.
*   **R5.1400** **If** the Manual Sync completes successfully, **then** the system **shall** display a success message and refresh the "Last Sync" time.
*   **R5.1500** **If** the Manual Sync fails, **then** the system **shall** display a specific, actionable error message (e.g., "Check Internet Connection").
