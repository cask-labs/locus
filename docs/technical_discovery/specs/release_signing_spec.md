# Release Signing Specification

This document defines the process for signing Android releases for Locus.

## Overview

Release builds (`release` build type) must be signed with a secure keystore to be installable on Android devices and accepted by app stores.
To ensure security and support reproducible builds in CI/CD, we inject the signing configuration via environment variables rather than checking the keystore into the repository.

## Signing Configuration

The signing configuration is located in `buildSrc/src/main/kotlin/com.locus.android-app.gradle.kts`.

It requires the following Environment Variables (or GitHub Secrets):

| Variable Name | Description |
| :--- | :--- |
| `LOCUS_UPLOAD_KEYSTORE_BASE64` | The Base64 encoded content of the `.jks` or `.keystore` file. |
| `LOCUS_KEY_ALIAS` | The alias of the key within the keystore. |
| `LOCUS_STORE_PASSWORD` | The password for the keystore. |
| `LOCUS_KEY_PASSWORD` | The password for the specific key. |

If these variables are missing or invalid, the build will proceed **without signing**, resulting in an "unsigned" APK that cannot be installed on a device.

## How to Setup Signing (For Developers)

To enable signed releases in your GitHub Actions workflow, you must generate a keystore and configure the repository secrets.

### 1. Generate the Keystore

We provide a helper script to generate a standard Upload Key and format the secrets for you.

Run the following command from the project root:

```bash
./scripts/generate_upload_keystore.sh
```

Follow the interactive prompts:
1.  **Key Alias**: Enter a name (default: `locus-upload`).
2.  **Store Password**: Enter a secure password.
3.  **Key Password**: Enter a secure password (or press enter to reuse the store password).

### 2. Configure GitHub Secrets

The script will output the exact values you need.

1.  Go to your GitHub Repository.
2.  Navigate to **Settings** -> **Secrets and variables** -> **Actions**.
3.  Click **New repository secret**.
4.  Add the four secrets output by the script:
    *   `LOCUS_UPLOAD_KEYSTORE_BASE64` (The long block of text)
    *   `LOCUS_KEY_ALIAS`
    *   `LOCUS_STORE_PASSWORD`
    *   `LOCUS_KEY_PASSWORD`

### 3. Verification

Trigger a new release build (e.g., by pushing a tag `vX.Y.Z`). Check the "Build Standard APK" step logs. You should **not** see the warning "Signing skipped".

## Security Notes

*   **Never commit the `.jks` file to Git.**
*   Store the generated `keystore_output/upload.jks` file in a secure password manager (e.g., 1Password, LastPass) as a backup.
*   The `LOCUS_UPLOAD_KEYSTORE_BASE64` secret allows the CI runner to reconstruct the keystore file temporarily in memory/disk during the build process.
