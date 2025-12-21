#!/bin/bash
set -e

echo "Starting Security Verification..."

# 1. Secret Scanning (Trufflehog)
echo "Running Trufflehog..."
trufflehog filesystem . --exclude-paths=scripts/trufflehog_excludes.txt || { echo "Trufflehog failed"; exit 1; }

# 2. IaC Scanning (Checkov)
echo "Running Checkov on CloudFormation..."
if [ -f "docs/technical_discovery/locus-stack.yaml" ]; then
    checkov -f docs/technical_discovery/locus-stack.yaml || { echo "Checkov failed"; exit 1; }
else
    echo "Warning: docs/technical_discovery/locus-stack.yaml not found. Skipping Checkov."
fi

# 3. SAST (Semgrep)
echo "Running Semgrep..."
semgrep scan --config=p/default || { echo "Semgrep failed"; exit 1; }

echo "Security Verification Passed."
