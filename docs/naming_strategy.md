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
*   **Examples:** `Pixel7`, `DadiPhone`, `BlueTruck`.
*   **Constraints:** Alphanumeric, distinct for that specific user.

### 2. AWS Resource: CloudFormation Stack
The app deploys a CloudFormation stack named `Locus-<DeviceName>`.
*   **Example:** `Locus-Pixel7`.
*   **Benefit:** This makes the stack easily identifiable in the AWS Console.

### 3. AWS Resource: S3 Bucket (The Physical ID)
The S3 bucket name is **auto-generated** by CloudFormation to guarantee uniqueness.
*   **Format:** `locus-<DeviceName>-<RandomSuffix>`
*   **Example:** `locus-pixel7-j4k2m9zp`
*   **Mechanism:** The Android app does *not* choose the bucket name. It passes the `DeviceName` as a parameter to the CloudFormation template. The template uses `Fn::Join` with a pseudo-parameter (like `AWS::StackId` or a randomization string) to construct the final bucket name.
*   **Discovery:** After provisioning, the app queries the Stack Outputs to discover the actual physical name of the bucket (`locus-pixel7-j4k2m9zp`) and saves it to local preferences.

## Multi-Tenancy (One Account, Many Devices)
This strategy natively supports multiple devices on a single AWS account.

*   **Device A** sets name "Pixel7" $\rightarrow$ Stack `Locus-Pixel7` $\rightarrow$ Bucket `locus-pixel7-a1b2`
*   **Device B** sets name "iPhone" $\rightarrow$ Stack `Locus-iPhone` $\rightarrow$ Bucket `locus-iphone-c3d4`

No collision occurs as long as the user provides unique *Device Names* for their own devices. If they try to reuse "Pixel7", CloudFormation will detect the existing stack and the app can prompt to "Link to existing" or "Choose new name".
