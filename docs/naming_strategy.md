# Resource Naming & Collision Strategy

This document defines how Locus handles the naming of AWS resources to ensure global uniqueness while maintaining user-friendly configuration, particularly when multiple devices share a single AWS account.

## The Challenge
*   **S3 Global Uniqueness:** S3 bucket names must be unique across *all* AWS accounts worldwide. A simple name like `locus-backup` is likely taken.
*   **Single Account, Multiple Devices:** A user may want to track multiple devices (e.g., "Personal Phone", "Work Phone", "Partner's Phone") using the same AWS Identity (IAM User). Each device needs its own independent stack to avoid data collision and simplify management.

## The Strategy: "Stack Name" vs. "Physical ID"

We separate the *User's Concept* of the device from the *AWS Physical ID*.

### 1. User Input: "Device Name" (Stack Name)
During onboarding, the user is asked for a **Device Name**.
*   **Purpose:** To identify this specific installation in the user's mental model.
*   **Default:** The app pre-fills this field with the system model name (e.g., `Pixel 7`, `Samsung S23`) to reduce friction.
*   **Uniqueness Constraint:** This name must be unique **within the user's AWS Account**. It does *not* need to be globally unique.

### 2. AWS Resource: CloudFormation Stack
The app deploys a CloudFormation stack named `Locus-<DeviceName>`.
*   **Example:** `Locus-Pixel7`.
*   **Benefit:** This makes the stack easily identifiable in the AWS Console.

### 3. AWS Resource: S3 Bucket (The Physical ID)
The S3 bucket name is **auto-generated** by CloudFormation to guarantee uniqueness.
*   **Format:** `locus-<DeviceName>-<RandomSuffix>`
*   **Example:** `locus-pixel7-j4k2m9zp`
*   **Mechanism:** The Android app does *not* choose the bucket name. It passes the `DeviceName` as a parameter to the CloudFormation template. The template uses `Fn::Join` with a pseudo-parameter (like `AWS::StackId` or a randomization string) to construct the final bucket name.
*   **Uniqueness Constraint:** This name is guaranteed to be **Globally Unique** by CloudFormation.

### 4. Discovery Strategy (Important)
For **System Recovery**, the app needs to find these buckets *without* knowing the random suffix.
*   **Method:** The app lists all buckets in the account and filters for those starting with `locus-`.
*   **Why:** This is faster and cheaper (1 API call) than querying CloudFormation Stacks or checking Tags on every bucket.
*   **Constraint:** Users must not manually rename their buckets to something that doesn't start with `locus-`, or the app will not find them.

## Multi-Tenancy (One Account, Many Devices)
This strategy natively supports multiple devices on a single AWS account.

*   **Device A** sets name "Pixel7" $\rightarrow$ Stack `Locus-Pixel7` $\rightarrow$ Bucket `locus-pixel7-a1b2`
*   **Device B** sets name "iPhone" $\rightarrow$ Stack `Locus-iPhone` $\rightarrow$ Bucket `locus-iphone-c3d4`

No collision occurs as long as the user provides unique *Device Names* for their own devices. If they try to reuse "Pixel7", CloudFormation will detect the existing stack and the app can prompt to "Link to existing" or "Choose new name".
