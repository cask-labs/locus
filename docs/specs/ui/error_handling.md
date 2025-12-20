# Error Handling & Feedback

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Related Requirements:** [UI Feedback](../../requirements/ui_feedback.md)

**Purpose:** Define how the application communicates status, errors, and critical failures to the user.

## 1. Global Feedback Patterns

### 1.1. Snackbars (Transient)
*   **Use Case:** Transient errors, non-blocking confirmations, and system status updates.
*   **Behavior:** Appears at the bottom of the screen, automatically dismisses after 4 seconds (Short) or 10 seconds (Long).
*   **Interaction:** Can contain a single text action (e.g., "RETRY").
*   **Examples:**
    *   "Network error. Retrying in 5s..." [RETRY NOW]
    *   "Sync Complete."
    *   "Map currently offline."

### 1.2. Dialogs (Blocking)
*   **Use Case:** Destructive actions or critical decisions requiring explicit user confirmation.
*   **Behavior:** Modal overlay that dims the background. User must choose an action or dismiss (if cancellable).
*   **Specifics:** See [Dashboard](dashboard.md) for "Stop Tracking" and [Settings](settings.md) for "Clear Buffer".

## 2. Tier 3 Fatal Errors

**Definition:** Critical failures where the application **cannot function** without user intervention.

**Presentation Styles:**
1.  **Blocking Full-Screen:** The default for system-critical issues (Auth, Permissions). Prevents navigation to other screens.
2.  **Dashboard Status Card:** For operational failures (e.g., Service Instability), the error is displayed within the Dashboard's Status Card, allowing the user to access Logs or Settings.

### 2.1. Permission Revoked
*   **Trigger:** The user (or OS) revokes `ACCESS_FINE_LOCATION` or `ACCESS_BACKGROUND_LOCATION`.

```text
+--------------------------------------------------+
|                                                  |
|              ( Location Off Icon )               |
|                                                  |
|            Location Permission Needed            |
|                                                  |
|      Locus requires "Always Allow" location      |
|      access to record your tracks in the         |
|      background.                                 |
|                                                  |
|      Please grant this permission in Settings.   |
|                                                  |
+--------------------------------------------------+
|            [ OPEN SETTINGS ]                     |
+--------------------------------------------------+
```

### 2.2. Authentication Failure (Runtime)
*   **Trigger:** The S3 Credentials (Runtime Keys) are invalid, expired, or deleted.
*   **Context:** This is different from Onboarding. This happens *after* the app was successfully set up.

```text
+--------------------------------------------------+
|                                                  |
|              ( Key Error Icon )                  |
|                                                  |
|            Authentication Failed                 |
|                                                  |
|      Locus cannot access your S3 bucket.         |
|      Your credentials may have expired or        |
|      been revoked.                               |
|                                                  |
|      You need to re-authenticate to continue.    |
|                                                  |
+--------------------------------------------------+
|            [ RECOVER ACCOUNT ]                   |
+--------------------------------------------------+
```

### 2.3. Storage Full / Critical IO Error
*   **Trigger:** The device runs out of disk space, or the database is corrupted.

```text
+--------------------------------------------------+
|                                                  |
|              ( Database Error Icon )             |
|                                                  |
|            Critical Storage Error                |
|                                                  |
|      Locus cannot save new data.                 |
|      Device storage may be full.                 |
|                                                  |
|      Please free up space on your device.        |
|                                                  |
+--------------------------------------------------+
|            [ TRY AGAIN ]                         |
+--------------------------------------------------+
```

### 2.4. Service Instability (Circuit Breaker)
*   **Trigger:** The Background Service has crashed or failed to start 3 consecutive times (Watchdog Circuit Breaker).
*   **Presentation:** **Dashboard Status Card** (Red).
*   **Action:** User must manually resume tracking to reset the circuit breaker.
*   **Specifics:** See [Dashboard](dashboard.md) for the "Service Instability" card layout.
