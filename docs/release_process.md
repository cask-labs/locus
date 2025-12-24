# Release Process

This document describes the step-by-step process for releasing a new version of the Locus application.

The release pipeline is fully automated via GitHub Actions, building both the `standard` (Google Play) and `foss` (F-Droid/GitHub) flavors simultaneously.

## 1. Preparation

Before triggering the release, you must prepare the codebase and release notes.

### Update Release Notes
The automated pipeline reads the release notes from a specific text file and publishes them to the "What's New" section on Google Play and the GitHub Release body.

1.  Edit the following file:
    ```
    app/src/main/play/release-notes/en-US/default.txt
    ```
2.  Replace the content with a description of the changes in this version.
    *   *Example:*
        ```text
        - Added new heatmap visualization for signal quality
        - Fixed battery drain issue on Pixel 7
        - Improved offline map caching performance
        ```
3.  Commit and push this change to the `main` branch.

## 2. Triggering the Release

The release process is triggered by pushing a **Git Tag** starting with `v` (e.g., `v1.0.0`, `v1.2.3`).

### Versioning Logic
The build system automatically derives the version from the Git state:
*   **Version Name:** Derived directly from the tag name (e.g., `1.0.0`).
*   **Version Code:** Derived from the total number of commits in the repository history.

### Command
Run the following commands in your terminal:

```bash
# 1. Ensure you are on the latest main
git checkout main
git pull origin main

# 2. Create the tag (replace vX.Y.Z with your version)
git tag v1.0.0

# 3. Push the tag to GitHub to trigger the action
git push origin v1.0.0
```

## 3. Automation Process

Once the tag is pushed, the **Release** GitHub Action (`.github/workflows/release.yml`) will start automatically.

It performs the following steps:
1.  **Build:** Compiles both `standardRelease` (AAB) and `fossRelease` (APK).
2.  **Sign:** Signs the artifacts using the Release Keystore (injected via secrets).
3.  **Publish (Google Play):**
    *   Uploads the `standard` AAB to the **Internal Test Track**.
    *   Uploads the Release Notes.
4.  **Publish (GitHub):**
    *   Creates a new GitHub Release for the tag.
    *   Attaches the `foss` APK, `standard` AAB, and SBOM.

## 4. Verification

You can monitor the progress in the **Actions** tab of the GitHub repository.

### Success Criteria
*   **Google Play Console:** A new release should appear in the **Internal testing** track.
*   **GitHub:** A new Release should be visible on the repository homepage with the attached APK and AAB.

## 5. Troubleshooting

If the release fails:
1.  Check the GitHub Action logs for errors.
2.  **Common Issues:**
    *   *Keystore Error:* Ensure `LOCUS_UPLOAD_KEYSTORE_BASE64` and related secrets are correctly set in GitHub.
    *   *Play Store API:* Ensure the Service Account (`LOCUS_PLAY_JSON`) has the correct permissions.
    *   *Version Code Conflict:* If you retag the same commit, the commit count (Version Code) will not change, causing Google Play to reject the update. You must add a new commit (e.g., update readme) before tagging again.
