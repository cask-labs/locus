# Persistent Notifications

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Related Requirements:** [UI Feedback](../../requirements/ui_feedback.md)

**Purpose:** Keep the service alive, prevent OS termination, and provide transparent status without opening the app.

## 1. Behavior & Philosophy
*   **Foreground Service:** The notification is anchored to a Foreground Service that runs as long as tracking is active or the service is initializing.
*   **Subtle by Default:** The notification must never vibrate or play sound unless a **Tier 3 Fatal Error** occurs.
*   **Visibility:** It must remain visible on the Lock Screen (public version, hiding sensitive content if configured by system).
*   **Action:**
    *   **"Stop Tracking":** The *only* action button available. This minimizes accidental interactions (like "Sync Now") from the lock screen and prevents unexpected battery usage.
    *   **Tap Behavior:** Tapping the notification body opens the **Dashboard**.

## 2. Content Format
The notification title and body must strictly follow this format to ensure consistency.

*   **Title:** `Locus • [State]`
*   **Body:** `[Tracking Status] • [Sync Status]`

## 3. States & Wireframes

### 3.1. Initializing (Starting Up)
*   **Context:** Service is starting, acquiring GPS, or connecting to DB.
*   **Style:** Standard (Low Priority).

```text
+--------------------------------------------------+
|  (Locus Icon)  Locus • Initializing              |
|  Acquiring GPS...                                |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.2. Active (Recording)
*   **Context:** Normal operation. GPS is locked, data is being recorded.
*   **Style:** Standard (Low Priority).
*   **Variations:**
    *   *High Accuracy:* GPS locked.
    *   *Power Saver:* Significant Motion only (Stationary).

```text
+--------------------------------------------------+
|  (Locus Icon)  Locus • Recording                 |
|  Tracking (High Accuracy) • Synced               |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.3. Active (Uploading)
*   **Context:** A background sync is in progress (Auto or Manual).
*   **Style:** Standard.

```text
+--------------------------------------------------+
|  (Locus Icon)  Locus • Recording                 |
|  Tracking • Uploading batch 1 of 3...            |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.4. Paused (Environmental - Tier 2)
*   **Context:** System paused due to environmental factors (Low Battery, Airplane Mode).
*   **Style:** **Standard** (Not Warning). The user expects this behavior based on settings.

```text
+--------------------------------------------------+
|  (Locus Icon)  Locus • Paused                    |
|  Low Battery (<15%) • Synced                     |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.5. User Stopped (Manual Stop)
*   **Context:** User explicitly stopped tracking via Dashboard or Notification.
*   **Style:** **Standard**.
*   **Note:** The service may stop the Foreground Service entirely, removing this notification. However, if the app chooses to keep a "Resume" state active, this is the format. *By default, Locus removes the notification on Stop.*

### 3.6. Error (Fatal - Tier 3)
*   **Context:** Critical failure requiring user intervention (Permission Revoked, Auth Failed).
*   **Style:** **High Priority** (May vibrate/sound depending on channel settings).
*   **Color:** System Error color (if supported).

```text
+--------------------------------------------------+
|  (Alert Icon)  Locus • Service Halted            |
|  Action Required: Permission Revoked             |
|                                                  |
|            [ OPEN APP ]                          |
+--------------------------------------------------+
```
