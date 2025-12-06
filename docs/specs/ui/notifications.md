# Persistent Notifications

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Related Requirements:** [UI Feedback](../../requirements/ui_feedback.md), [Background Processing](../background_processing_spec.md)

**Purpose:** Keep the service alive, prevent OS termination, and provide transparent status without opening the app.

## 1. Behavior & Philosophy

### 1.1. Notification Channels
To adhere to the "Subtle by Default" philosophy while ensuring critical alerts are seen, the application must implement two distinct notification channels.

*   **Tracking Status (`channel_tracking`)**
    *   **Importance:** `LOW` (Silent, Minimized, No Vibration).
    *   **Usage:** Routine operations (Recording, Uploading, Environmental Pauses).
    *   **Behavior:** Appears in the "Silent" section of the notification shade.
*   **Critical Alerts (`channel_alerts`)**
    *   **Importance:** `HIGH` (Heads-up, Sound + Vibration allowed).
    *   **Usage:** Tier 3 Fatal Errors (e.g., Watchdog Rescue, Permission Revoked).
    *   **Behavior:** May pop up on screen and play sound/vibrate (respecting system DND).

### 1.2. Core Rules
*   **Foreground Service:** The notification is anchored to a Foreground Service that runs as long as tracking is active, initializing, or stopping.
*   **Visibility:** It must remain visible on the Lock Screen (public version, hiding sensitive content if configured by system).
*   **Action Strategy:**
    *   **"Stop Tracking":** The primary action for active states. This minimizes accidental interactions.
    *   **"Resume Tracking":** Strictly reserved for the **Watchdog Rescue** state.
    *   **Tap Behavior:** Tapping the notification body opens the **Dashboard** (or deep-links to the error resolution screen for Fatal Errors).

## 2. Content Format
The notification title and body must strictly follow this format to ensure consistency.

*   **Title:** `Locus • [State]`
*   **Body:** `[Tracking Status] • [Sync Status]`

## 3. States & Wireframes

### 3.1. Initializing (Starting Up)
*   **Context:** Service is starting, acquiring GPS, or connecting to DB.
*   **Channel:** `channel_tracking` (Low).
*   **Icon:** `@drawable/ic_stat_tracking` (Animated/Pulsing if supported, otherwise Static).

```text
+--------------------------------------------------+
|  (ic_stat_tracking)  Locus • Initializing        |
|  Acquiring GPS...                                |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.2. Active (Recording)
*   **Context:** Normal operation. GPS is locked, data is being recorded.
*   **Channel:** `channel_tracking` (Low).
*   **Icon:** `@drawable/ic_stat_tracking`
*   **Variations:**
    *   *High Accuracy:* GPS locked.
    *   *Stationary:* Significant Motion only (GPS Suspended).
    *   *Buffer Warning:* **Warning:** Local buffer > 10k items (Risk of data loss).

```text
+--------------------------------------------------+
|  (ic_stat_tracking)  Locus • Recording           |
|  Tracking (High Accuracy) • Synced               |
|  (OR: Tracking • Buffer Warning: 10k+ pts)       |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.3. Active (Uploading)
*   **Context:** A background sync is in progress (Auto or Manual).
*   **Channel:** `channel_tracking` (Low).
*   **Icon:** `@drawable/ic_stat_sync` (or `ic_stat_tracking`)

```text
+--------------------------------------------------+
|  (ic_stat_sync)      Locus • Recording           |
|  Tracking • Uploading batch 1 of 3...            |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.4. Paused (Environmental - Tier 2)
*   **Context:** System paused due to environmental factors (Low Battery < 15%, Airplane Mode).
*   **Channel:** `channel_tracking` (Low).
*   **Icon:** `@drawable/ic_stat_paused`
*   **Style:** **Standard** (Not Warning).

```text
+--------------------------------------------------+
|  (ic_stat_paused)    Locus • Paused              |
|  Low Battery (<15%) • Synced                     |
|                                                  |
|            [ STOP TRACKING ]                     |
+--------------------------------------------------+
```

### 3.5. Stopping (Transient)
*   **Context:** User tapped "Stop Tracking". Service is flushing buffer and releasing resources (approx. 1-3s).
*   **Channel:** `channel_tracking` (Low).
*   **Icon:** `@drawable/ic_stat_tracking`
*   **Action:** None (Buttons removed to prevent double-taps).

```text
+--------------------------------------------------+
|  (ic_stat_tracking)  Locus • Stopping...         |
|  Saving final location...                        |
|                                                  |
|                                                  |
+--------------------------------------------------+
```

### 3.6. Watchdog Rescue (Tier 3 - Android 12+)
*   **Context:** Service died/was killed, and Android 12+ prevents silent background restart.
*   **Channel:** `channel_alerts` (High).
*   **Icon:** `@drawable/ic_stat_alert`
*   **Action:** Explicit "RESUME" button to satisfy OS requirements.

```text
+--------------------------------------------------+
|  (ic_stat_alert)     Locus • Service Halted      |
|  Tracking Paused Unexpectedly                    |
|                                                  |
|            [ RESUME TRACKING ]                   |
+--------------------------------------------------+
```

### 3.7. Error (Fatal - Tier 3)
*   **Context:** Critical failure requiring user intervention (Permission Revoked, Auth Failed, Circuit Breaker).
*   **Channel:** `channel_alerts` (High).
*   **Icon:** `@drawable/ic_stat_alert`
*   **Color:** System Error color (Red).
*   **Tap Behavior:** Deep link to the specific resolution screen (e.g., App Settings for Permissions) or open Dashboard with Error Modal.

```text
+--------------------------------------------------+
|  (ic_stat_alert)     Locus • Service Halted      |
|  Action Required: Permission Revoked             |
|                                                  |
|            [ OPEN APP ]                          |
+--------------------------------------------------+
```
