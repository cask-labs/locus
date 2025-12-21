# Google Play Store Compliance Analysis

This document analyzes the Locus application architecture, user flows, and permissions against Google Play Store policies. It identifies potential risks for rejection and provides recommendations for mitigation.

## 1. High-Risk Areas

### 1.1. Background Location Access
**Risk Level:** **Critical**
**Policy:** Apps requesting `ACCESS_BACKGROUND_LOCATION` must demonstrate a clear value proposition to the user, the feature must be critical to the app's core functionality, and users would expect the app to access this data in the background.
**Analysis:** Locus is a location tracker; thus, background location is its core feature. However, Google is increasingly strict.
**Mitigation Strategy:**
*   **Prominent Disclosure:** The app MUST display a prominent disclosure *before* the system permission dialog. This disclosure must explain *what* data is used, *how* it is used, and *why* it is necessary in the background. It must include the exact phrase "location" and be visible without scrolling.
*   **Video Demonstration:** A video showing the feature in action, including the disclosure and the background execution, must be submitted to the Play Console.
*   **Select "Always Allow":** The app must guide the user to select "Allow all the time" in settings.

### 1.2. Handling of AWS Credentials (Secret Keys)
**Risk Level:** **High**
**Policy:** Google prohibits apps that attempt to steal credentials or compromise user accounts. Asking for a "Secret Access Key" looks suspicious to automated scanners and human reviewers.
**Analysis:** The "Bring Your Own Cloud" model requires these keys. To a reviewer, it might look like a phishing attempt or a security vulnerability.
**Mitigation Strategy:**
*   **Clear Branding:** Emphasize "Sovereign" and "Client-side only".
*   **Documentation:** Provide a clear "Privacy Policy" and "App Content" declaration explaining that keys are stored locally (Encrypted SharedPreferences/Keystore) and used only to communicate directly with AWS.
*   **Labeling:** Ensure the UI makes it clear this is for *their* account, not a login to a service *we* own. Use terms like "Connect *Your* AWS Account".
*   **Transparency:** If possible, link to the source code or documentation that proves the keys don't leave the device.

### 1.3. Battery Saver Exemptions
**Risk Level:** **Medium**
**Policy:** Apps should respect system power management. Using `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is restricted to specific use cases (like automation apps, alarm clocks, or health trackers).
**Analysis:** Locus requires continuous tracking. The documentation implies overriding battery saver modes.
**Mitigation Strategy:**
*   **Justification:** When submitting, select the category "Tracking or health apps" to justify the exemption.
*   **User Consent:** Do not try to bypass it silently. Use the proper intent to ask the user to add the app to the whitelist.
*   **Graceful Degradation:** If the user refuses, the app should still function (perhaps with reduced frequency) rather than crashing or blocking.

### 1.4. Foreground Service Permissions
**Risk Level:** **Medium**
**Policy:** Android 14+ restricts Foreground Services. `FOREGROUND_SERVICE_LOCATION` is required.
**Analysis:** Locus uses a Foreground Service for tracking.
**Mitigation Strategy:**
*   **Manifest:** Declare `android:foregroundServiceType="location"`.
*   **Notification:** The persistent notification must be visible while tracking.
*   **Runtime Check:** Ensure `startForeground` is called correctly to avoid `ForegroundServiceStartNotAllowedException`.

### 1.5. "Sensitive" Data & Privacy Policy
**Risk Level:** **Medium**
**Policy:** All apps must have a valid Privacy Policy URL. It must disclose data access, collection, use, and sharing.
**Analysis:** Even though data is stored on the user's S3, the *app* collects it.
**Mitigation Strategy:**
*   **Policy Content:** The policy must explicitly state: "Locus collects location data to enable [feature] even when the app is closed or not in use."
*   **No Third-Party Transfer:** Explicitly state that data is transferred *only* to the user's configured AWS S3 bucket and not to the developer (except for optional opt-in telemetry).

### 1.6. Use of `SCHEDULE_EXACT_ALARM`
**Risk Level:** **Medium**
**Policy:** Restricted permission. Google requires apps to use inexact alarms unless there's a compelling reason.
**Analysis:** If used for "periodic burst sampling" or "watchdog", it might be flagged.
**Mitigation Strategy:**
*   **Evaluation:** Check if `setWindow` or `setRepeating` (inexact) suffices. If "Watchdog" requires exact timing, justify it as a "Time-critical" function, though this is weak for a tracker.
*   **Recommendation:** Prefer `WorkManager` for reliability over exact alarms where possible, or accept inexact timing for the watchdog.

### 1.7. Competitor Comparison: Google Location History
**Context:** Reviewers may ask, "Why do you need this when Google Maps already does it?"
**Data Point:**
*   **Google Location History (Timeline):** In the background, standard Android behavior throttles location updates to "a few times per hour" (approx. every 10-15 minutes) or relies on significant motion changes. It prioritizes battery over precision, often relying on WiFi/Cell towers (100m+ accuracy).
*   **Locus:** Records at **1Hz (1 second)** intervals using a Foreground Service with High Accuracy GPS.
**Justification Strategy:**
*   **Differentiation:** Explicitly state in the Play Console declaration that Locus provides *granular* data (speed, exact path, cornering) that Google Timeline misses.
*   **Use Case:** "Travel logging, photography geotagging, and speed analysis requiring second-by-second precision unavailable in standard operating system history."

## 2. Recommendation Checklist

1.  [ ] **Implement Prominent Disclosure:** Create a specific UI screen before the permission request.
2.  [ ] **Privacy Policy URL:** Host a static page (e.g., GitHub Pages) with the required legal text.
3.  [ ] **Safety Section:** Fill out the Data Safety form in Play Console carefully. Mark "Location" as "Collected" (because the app processes it off-device to S3) but "Not Shared" with the developer.
4.  [ ] **Restricted Permission Form:** Prepare the text for the Background Location permission declaration.
5.  [ ] **Credential UI:** Add "Learn More" links and "Why do we need this?" tooltips next to the AWS Secret Key input.
6.  [ ] **Battery Optimization:** Ensure the app handles the "Deny" case for battery optimization gracefully.

## 3. Potential Rejection Reasons Summary

| Reason | Probability | Mitigation |
| :--- | :--- | :--- |
| **Malware/Phishing (AWS Keys)** | Medium | Clear UI copy, "Your Cloud" branding, Privacy Policy. |
| **Background Location (Policy)** | High | Perfect "Prominent Disclosure" & Video. |
| **Battery Exemption Abuse** | Low/Medium | Proper category selection in Console. |
| **Broken Functionality (if perms denied)** | Low | Ensure app doesn't crash if permissions are denied. |
