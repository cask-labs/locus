#!/bin/bash
set -e

# Defaults
FLAVOR="standard"
BUILD_TYPE="release"

# Parse Arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --flavor) FLAVOR="$2"; shift ;;
        --build-type) BUILD_TYPE="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

# Capitalize first letter for Gradle task naming (e.g., standard -> Standard)
# Using python for reliable capitalization as bash behavior varies
FLAVOR_CAP=$(python3 -c "print('$FLAVOR'.capitalize())")
BUILD_TYPE_CAP=$(python3 -c "print('$BUILD_TYPE'.capitalize())")

TASK_NAME="assemble${FLAVOR_CAP}${BUILD_TYPE_CAP}"

echo "Building Artifacts..."
echo "Flavor: $FLAVOR"
echo "Build Type: $BUILD_TYPE"
echo "Task: $TASK_NAME"

# 1. Run Gradle Task
./gradlew "$TASK_NAME" || { echo "Build failed"; exit 1; }

# 2. Move Artifacts
mkdir -p dist
# Determine path based on conventions
# App: app/build/outputs/apk/standard/release/app-standard-release.apk (Example)
# FOSS: app/build/outputs/apk/foss/release/app-foss-release.apk (Example)
# Need to be generic enough.
# Let's find the apk.
APK_PATH=$(find app/build/outputs/apk -name "*.apk" | grep "$FLAVOR" | grep "$BUILD_TYPE" | head -n 1)

if [ -n "$APK_PATH" ]; then
    echo "Found APK: $APK_PATH"
    cp "$APK_PATH" dist/
    echo "Artifact copied to dist/"
else
    echo "Error: Could not locate generated APK."
    exit 1
fi

echo "Build Artifacts Complete."
