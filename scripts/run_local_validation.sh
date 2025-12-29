#!/bin/bash
set -e

echo "Starting Local Validation Suite (Tier 2)..."

# 1. Gradle Checks
echo "Running Lint, Unit Tests, and Ktlint..."

# Determine Ktlint task based on environment
if [ "$CI" = "true" ]; then
    echo "CI Environment detected. Running ktlintCheck..."
    KTLINT_TASK="ktlintCheck"
else
    echo "Local Environment detected. Running ktlintFormat..."
    KTLINT_TASK="ktlintFormat"
fi

# Using || exit 1 to ensure script fails if any gradle task fails
./gradlew lintDebug test "$KTLINT_TASK" || { echo "Gradle checks failed"; exit 1; }

echo "Running Kover Verification..."
# This ensures that branch coverage thresholds are met
./gradlew koverVerify || { echo "Coverage verification failed"; exit 1; }

echo "Running Mutation Testing (PIT)..."
# Ensures that tests are killing mutations (75% threshold)
./gradlew :core:domain:pitest || { echo "Mutation testing failed"; exit 1; }

# 2. Security Checks
echo "Running Security Verification..."
./scripts/verify_security.sh || { echo "Security verification failed"; exit 1; }

echo "Local Validation Suite Passed."
