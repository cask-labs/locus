#!/bin/bash
set -e

echo "Starting Security Verification..."

# 1. Secret Scanning (Trufflehog)
echo "Running Trufflehog..."
trufflehog filesystem . --exclude-paths=scripts/trufflehog_excludes.txt || { echo "Trufflehog failed"; exit 1; }

# 2. IaC Scanning (Checkov)
echo "Running Checkov on CloudFormation..."
if [ -f "core/data/src/main/assets/locus-stack.yaml" ]; then
    # Skip CKV_AWS_40: Per-device isolation architecture requires direct user attachment to IAM users
    # See: docs/adr/iam-policy-attachment-per-device-isolation.md
    checkov -f core/data/src/main/assets/locus-stack.yaml --skip-check CKV_AWS_40 || { echo "Checkov failed"; exit 1; }
else
    echo "Warning: core/data/src/main/assets/locus-stack.yaml not found. Skipping Checkov."
fi

# 3. SAST (Semgrep)
echo "Running Semgrep..."
semgrep scan --config=p/default || { echo "Semgrep failed"; exit 1; }

echo "Security Verification Passed."
