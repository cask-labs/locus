# Critique & Recommendations: Phase 1 - Onboarding & Identity

## Executive Summary
This document outlines three critical architectural flaws identified in the Phase 1 discovery artifacts (`01` through `05`) and provides concrete recommendations to resolve them. The primary goals of these changes are to ensure **Infrastructure Consistency**, **Least Privilege Security**, and **Android 14+ Compliance**.

---

## Issue 1: The "Split Brain" IAM Strategy

### The Flaw
The current architecture proposes two conflicting methods for managing IAM users, creating a "Split Brain" scenario:
*   **Path A (New Setup):** Uses CloudFormation (`locus-stack.yaml`) to create the `LocusUser` and `LocusAccessKey`. This is "Infrastructure as Code" (IaC).
*   **Path B (Recovery):** The `onboarding.md` and `RecoverAccountUseCase` instruct the app to imperatively call `iam:CreateUser` directly via the AWS SDK to generate new credentials for an existing bucket.

### Why this is Critical
1.  **Infrastructure Drift:** The "Recovery User" exists outside of CloudFormation. If the stack is updated or deleted later, this user will remain as an orphan "zombie" credential.
2.  **Security Debt:** Unmanaged IAM users are a primary vector for credential leakage.
3.  **Code Duplication:** The app requires two distinct logic paths for user creation (Parameter-based CFN deployment vs. Imperative SDK calls).
4.  **Permission Bloat:** The `iam-bootstrap-policy.json` must grant broad `iam:CreateUser` permissions to the app, effectively making it an IAM Admin.

### Recommendation: "Satellite Stack" Pattern
**Standardize on CloudFormation for ALL user creation.**

Instead of calling `iam:CreateUser` directly during recovery, the app should deploy a lightweight **"Satellite Stack"** (e.g., `locus-access-stack.yaml`).
*   **Input:** `BucketName` (Existing), `DeviceName` (New).
*   **Resources:** Defines only `AWS::IAM::User`, `AWS::IAM::AccessKey`, and `AWS::IAM::Policy`.
*   **Outcome:** The "Recovery User" is now a fully managed CloudFormation resource.
*   **Benefit:** Uniform code path (always "Deploy Stack"), no zombie users, and the bootstrap policy can be tightened to only allow `cloudformation:*`.

---

## Issue 2: The "List Buckets" Discovery Fallacy

### The Flaw
The `onboarding.md` specification relies on `s3:ListBuckets` and client-side filtering (checking for `locus-` prefix) to discover existing stores.

### Why this is Critical
1.  **Reliability:** CloudFormation (by default) generates unique physical names for buckets (e.g., `stack-locusdatabucket-x9z8y7`). These names are NOT guaranteed to have a predictable prefix unless manually hardcoded (which risks global collision). The current discovery logic would likely find *zero* buckets.
2.  **Privacy & Security:** Granting `s3:ListBuckets` allows the application to list *every* bucket in the user's AWS account, including personal or work buckets unrelated to Locus. This violates the principle of Least Privilege.

### Recommendation: Tag-Based Discovery
**Leverage the Resource Groups Tagging API.**

The `locus-stack.yaml` already applies specific tags to the bucket:
```yaml
Tags:
  - Key: LocusRole
    Value: DeviceBucket
```

The discovery flow should be updated to:
1.  **Permission:** Replace `s3:ListBuckets` with `tag:GetResources` in the bootstrap policy.
2.  **Logic:** Query the AWS Resource Groups Tagging API for resources where `Tag.LocusRole == 'DeviceBucket'`.
3.  **Benefit:** This method is robust (independent of naming conventions), precise (returns only Locus buckets), and secure (hides irrelevant resources).

---

## Issue 3: Foreground Service Compliance (Android 14+)

### The Flaw
The selected architecture (`Path B`) relies on a **Foreground Service** with the type `dataSync` to keep the app alive during the 3-5 minute CloudFormation provisioning process.

### Why this is Critical
1.  **Strict Enforcement:** Android 14+ strictly enforces Foreground Service types. `dataSync` is intended for media/file transfer and has system-imposed timeouts (often ~3 minutes).
2.  **Process Death:** If the stack creation exceeds the timeout, or if the OS determines "no data is actually syncing" (since the app is mostly just polling an API), the system may kill the service. This catastrophic failure mid-provisioning would leave the user in an inconsistent state.
3.  **User Experience:** Relying on the user to "keep the app open" is fragile.

### Recommendation: WorkManager (Long-Running)
**Adopt `WorkManager` with `Expedited` / `LongRunning` support.**

While `Path C` was initially rejected due to "Security/Serialization" concerns, these can be mitigated to provide a superior architectural solution:
1.  **Security:** Instead of serializing Bootstrap Keys into the `WorkRequest` data (disk), store them in `EncryptedSharedPreferences` immediately upon entry. Pass only the *key alias* or *flag* to the Worker.
2.  **Resilience:** WorkManager is the Google-recommended solution for "guaranteed execution". It handles retries, backoff, and process resurrection natively.
3.  **UX:** Use `setForeground()` within the Worker to show the required notification. This effectively gives us a Foreground Service *managed by the system*.
4.  **Benefit:** This aligns perfectly with Android 14+ best practices, removes the risk of arbitrary service kills, and simplifies the lifecycle management code.
