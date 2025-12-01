# Onboarding & Setup

**Goal:** Transition from a fresh install to a fully provisioned, cloud-connected system.

This flow covers both **New Installations** and **Recovery/Reconnection** to existing data stores.

## Step 0: Prerequisites & Documentation
Before entering any keys, the app educates the user on the security model.
*   **Action:** The user launches the app for the first time.
*   **UI:** A landing page explaining that Locus is "Bring Your Own Cloud".
*   **Guidance:** A prominent link: *"How to generate secure AWS keys"* points to the [Infrastructure Documentation](../infrastructure.md).
    *   *Recommendation:* The docs explicitly recommend using **AWS CloudShell** to generate temporary 1-hour session tokens for maximum security.

## Step 1: Authentication & Validation
The user provides the keys necessary to access their AWS account.

1.  **Entry:** User enters `Access Key ID`, `Secret Access Key`, and optionally `Session Token`.
    *   *UX:* Fields support clipboard paste and "Show/Hide" for secrets.
2.  **Validation:** The app performs a "Dry Run" check.
    *   *Action:* Calls `sts:GetCallerIdentity` to verify credentials are active.
    *   *Action:* Calls `s3:ListBuckets` to verify permission scope.
3.  **Result:**
    *   **Success:** Proceed to Step 2.
    *   **Failure:** Specific error message (e.g., "Invalid Signature", "Permission Denied") is shown.

## Step 2: The Choice
Now that the app can see the account, the user decides the path.

*   **Option A: Setup New Device**
    *   *Use Case:* "I want to start tracking this phone as a new entity."
*   **Option B: Link Existing Store**
    *   *Use Case:* "I want to restore my history from a previous install."

---

## Path A: Setup New Device (Provisioning)

1.  **Identity:** User enters a **Device Name** (e.g., "Pixel7").
    *   *Default:* Pre-filled with system model name.
    *   *Check:* App verifies this name is **unique** in the AWS account. If `Locus-Pixel7` already exists, the app **refuses** to proceed and asks for a different name (e.g., "Pixel7-Work").
2.  **Deploy:** User taps "Deploy Infrastructure".
    *   *Action:* App uses Bootstrap Keys to run CloudFormation.
    *   *Feedback:* "Provisioning Locus Store... (this may take 2 minutes)".
    *   *Resilience:* This process runs in a **Foreground Service** with a visible notification ("Provisioning Cloud Resources..."). This ensures the process completes even if the user switches apps or the screen turns off.
3.  **Key Swap:**
    *   App creates a new **IAM User** (e.g., `LocusUser_Pixel7`) using the Bootstrap Keys.
    *   App generates an Access Key for this user.
    *   App attaches a restricted policy (Bucket Access Only) to this user.
    *   App saves these **Runtime Keys** and discards the Bootstrap Keys.
4.  **Permissions:** The "Two-Step Dance" for Location Permissions.
    *   *Phase A:* Request "While Using".
    *   *Phase B:* Request "Allow all the time" (Background).
5.  **Outcome:** The user is navigated to the Dashboard, and the Onboarding screens are removed from the navigation stack. Tracking begins.

---

## Path B: Link Existing Store (Recovery)

1.  **Discovery:** App displays a list of detected Locus stores (e.g., `Pixel7`, `iPhone`).
    *   *Mechanism:* Filters `s3:ListBuckets` for `locus-*` prefix.
2.  **Selection:** User taps the store they want to link.
3.  **Key Swap (New User):**
    *   App creates a **New IAM User** for this device (e.g., `LocusUser_Pixel7_Recovery`).
    *   *Note:* The app does **not** retrieve old keys from the existing stack. This ensures that the new device has its own unique, revocable credentials without interfering with prior installations.
    *   App saves the new Runtime Keys and discards the Bootstrap Keys.
4.  **New Identity:**
    *   App generates a fresh `device_id` (e.g., `Pixel7_recovery_x9`).
    *   **Why? (Conflict Prevention):** If the original device (e.g., the old phone) is turned back on, both devices would try to upload files with the same `device_id`. Since filenames are timestamp-based, this could lead to a "Split Brain" scenario where one device overwrites the data of the other. A unique ID ensures both streams are preserved safely.
5.  **Lazy Sync:**
    *   App scans the bucket inventory to populate the calendar.
    *   **No massive download** occurs. Tracks are fetched on demand.
6.  **Outcome:** The user is navigated to the Dashboard, and the Onboarding screens are removed from the navigation stack. History is available; new tracking begins appended to the store.
