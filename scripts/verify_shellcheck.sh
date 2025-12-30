#!/bin/bash
set -e

# Function to check for ShellCheck
check_shellcheck() {
    if ! command -v shellcheck > /dev/null 2>&1; then
        echo "Error: shellcheck is not installed."
        echo "Please install it manually or use setup_ci_env.sh for CI."
        echo "Instructions: https://github.com/koalaman/shellcheck#installing"
        exit 1
    fi
}

echo "Starting ShellCheck Verification..."
check_shellcheck

# Find all .sh files and run shellcheck
# Using -print0 and xargs -0 to handle filenames with spaces
find . -type f -name "*.sh" -not -path "./.git/*" -print0 | xargs -0 shellcheck

echo "ShellCheck Verification Passed."
