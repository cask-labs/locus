# Deep Dive: KeySerial & IAM Credential Rotation

**Analysis Date:** 2024-05-21
**Context:** Detailed explanation of the `KeySerial` mechanism proposed for the "Takeover" Recovery Strategy.

## The Core Problem: Idempotency vs. Security
In a standard CloudFormation `UpdateStack` operation, the engine calculates the difference between the current state and the desired state.
- **Scenario:** A user loses their phone. They install Locus on a new phone and perform "Recovery".
- **Action:** The app finds the existing stack `locus-user-123` and calls `UpdateStack`.
- **The Issue:** If the user was a "Standard" user (`IsAdmin=false`) and recovers as a "Standard" user (`IsAdmin=false`), **nothing has changed** in the template definition.
- **CloudFormation Result:** "No updates are to be performed."
- **Security Consequence:** The existing `AWS::IAM::User` and its `AWS::IAM::AccessKey` remain active. The new device does not get new keys (because no new output was generated), and the old device (potentially stolen) retains valid access to the bucket.

## The Solution: Forced Resource Replacement via `KeySerial`

To secure the account, we must force CloudFormation to **destroy** the old Identity (User + Keys) and **create** a fresh one. We achieve this by exploiting CloudFormation's **Update Behaviors**.

### 1. The Mechanism
In CloudFormation, changing certain properties of a resource triggers a **Replacement** (Destroy + Create) rather than an in-place modification.
- For `AWS::IAM::User`, changing the `UserName` property requires **Replacement**.

We introduce a `KeySerial` parameter solely to control this property.

### 2. Implementation Details

**A. CloudFormation Template (`locus-stack.yaml`)**

```yaml
Parameters:
  StackName:
    Type: String
    Description: Unique Base ID for the device installation.
  KeySerial:
    Type: String
    Default: "1"
    Description: A nonce value. Changing this forces IAM User rotation.

Resources:
  LocusUser:
    Type: AWS::IAM::User
    Properties:
      # We construct the UserName using the Serial.
      # e.g., "locus-user-Pixel7-uuid1" -> "locus-user-Pixel7-uuid2"
      UserName: !Sub "locus-user-${StackName}-${KeySerial}"

  LocusAccessKey:
    Type: AWS::IAM::AccessKey
    Properties:
      UserName: !Ref LocusUser
      # Because LocusUser is replaced, LocusAccessKey (which depends on it)
      # is ALSO replaced automatically.
```

**B. Client Logic (`RecoverAccountUseCase`)**

When the app detects a recovery scenario (user selected a bucket to takeover):

1.  **Generate Serial:** The app generates a new random UUID (e.g., `550e8400-e29b...`).
2.  **Execute Update:**
    ```kotlin
    stackProvisioningService.updateAndPollStack(
        stackName = existingStackName,
        parameters = mapOf(
            "StackName" to existingBaseName,
            "IsAdmin" to "false", // Enforce downgrade
            "KeySerial" to newUuid // Force rotation
        )
    )
    ```

### 3. The Execution Flow

1.  **CloudFormation receives Update:** Sees that `KeySerial` has changed.
2.  **Diff Calculation:** Calculates that `LocusUser.UserName` will change from `...-uuid1` to `...-uuid2`.
3.  **Replacement Plan:** Marks `LocusUser` for **Replacement**.
4.  **Dependency Chain:**
    - `LocusAccessKey` depends on `LocusUser`. It is marked for Replacement.
    - `LocusPolicy` depends on `LocusUser`. It is updated to attach to the new user.
5.  **Execution:**
    - Creates New User (`...-uuid2`).
    - Creates New Access Key.
    - Updates Policy.
    - **Deletes Old Access Key** (Invalidating lost device).
    - **Deletes Old User** (`...-uuid1`).
6.  **Output:** Returns the *new* `AccessKeyId` and `SecretAccessKey`.

### 4. Why this is Robust
- **Atomic:** The switch happens within the CloudFormation transaction. There is no point where the bucket is left without a user (CloudFormation creates the new one before deleting the old one).
- **Stateless:** The client doesn't need to know the *previous* serial. It just needs to provide a *new* unique one (UUID).
- **Clean:** It uses standard AWS behavior rather than custom Lambda functions or complex scripts.

## Summary
The `KeySerial` is a "Big Red Button" that allows the Android client to cryptographically assert: **"I am the new owner. Burn the old keys and give me new ones."** without needing to know what the old keys were.
