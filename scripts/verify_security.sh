#!/bin/bash
set -e

echo "Starting Security Verification..."

# 1. Secret Scanning (Trufflehog)
echo "Running Trufflehog..."
trufflehog filesystem . --exclude-paths=scripts/trufflehog_excludes.txt || { echo "Trufflehog failed"; exit 1; }

# 2. IaC Scanning (Checkov)
echo "Running Checkov on CloudFormation..."
LOCUS_STACK_PATH="core/data/src/main/assets/locus-stack.yaml"
if [ -f "$LOCUS_STACK_PATH" ]; then
    # Skip CKV_AWS_40: Per-device isolation architecture requires direct user attachment to IAM users
    # See: docs/adr/iam-policy-attachment-per-device-isolation.md
    checkov -f "$LOCUS_STACK_PATH" --skip-check CKV_AWS_40 || { echo "Checkov failed"; exit 1; }
else
    echo "Warning: $LOCUS_STACK_PATH not found. Skipping Checkov."
fi

# 3. SAST (Semgrep)
echo "Running Semgrep..."
semgrep scan --config=p/default || { echo "Semgrep failed"; exit 1; }

echo "Security Verification Passed."
