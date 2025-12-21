#!/bin/bash
set -e

echo "Starting Local Validation Suite (Tier 2)..."

# 1. Gradle Checks
echo "Running Lint, Unit Tests, and Ktlint..."
# Using || exit 1 to ensure script fails if any gradle task fails
./gradlew lintDebug testDebugUnitTest ktlintCheck || { echo "Gradle checks failed"; exit 1; }

# 2. Security Checks
echo "Running Security Verification..."
./scripts/verify_security.sh || { echo "Security verification failed"; exit 1; }

echo "Local Validation Suite Passed."
