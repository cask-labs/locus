# System Recovery & Reconnection

**Goal:** Restore access to an existing Locus Data Store on a new installation.

## Step 0: The Choice
This flow begins when the user selects **"Recover / Link Existing"** on the initial launch screen.

## Step 1: Authentication (The Bootstrap)

The user must provide AWS credentials to allow Locus to find the existing bucket.

### The Secure Standard (Recommended)
**Use temporary credentials generated via AWS CloudShell.**
*   **Expiration:** These keys automatically self-destruct in 1 hour.
*   **Process:**
    1.  User logs into AWS Console on their computer.
    2.  User opens **CloudShell** (terminal icon).
    3.  User runs: `aws sts get-session-token --duration-seconds 3600`
    4.  User enters the 3 resulting values into Locus:
        *   `AccessKeyId`
        *   `SecretAccessKey`
        *   `SessionToken`

*(Note: While permanent keys are technically possible, the app strongly encourages the use of temporary session tokens for this sensitive operation.)*

---

## Step 2: Discovery

Locus scans the user's AWS account to find compatible Data Stores.

*   **Action:** App calls `s3:ListBuckets`.
*   **Filter:** App looks for buckets starting with `locus-`.
*   **User Interface:**
    *   **Success:** Displays a list of found stores (e.g., `locus-pixel7-RT5`, `locus-backup-99X`).
    *   **Failure:** If no buckets are found, the app prompts the user to switch to the **Onboarding (New Setup)** flow.

---

## Step 3: Selection & Link

1.  **Selection:** User taps the desired bucket (store) to link.
2.  **Key Swap:** The app automatically performs the secure "Key Swap" to retrieve restricted Runtime credentials. (See [Infrastructure: Security Architecture](../infrastructure.md#security-architecture-bootstrap-vs-runtime)).
3.  **Identity:** The app generates a new unique Device ID to safely append data to this store without overwriting existing history. (See [Data Strategy: Identity](../data_strategy.md#identity--write-patterns)).

---

## Step 4: Synchronization

The app performs a "Lazy Load" of the history.
*   **Immediate Result:** The History Calendar populates with indicators showing which days have data.
*   **Action:** No massive download occurs. Tracks are fetched only when viewed.
