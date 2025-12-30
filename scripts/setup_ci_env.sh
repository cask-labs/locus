#!/bin/bash
set -e

echo "Starting CI Environment Setup..."

# 1. Check for Python 3
if ! command -v python3 &> /dev/null; then
    echo "Error: python3 could not be found."
    exit 1
fi
echo "Python 3 is available."

# 2. Install Python Dependencies
echo "Installing Python dependencies from scripts/requirements.txt..."
python3 -m pip install -r scripts/requirements.txt
echo "Dependencies installed."

# 3. Verify Trufflehog
if ! command -v trufflehog &> /dev/null; then
    echo "Error: trufflehog is not installed."
    echo "Please install it: https://github.com/trufflesecurity/trufflehog"
    exit 1
fi
echo "trufflehog is available."

# 4. Install ShellCheck
if ! command -v shellcheck &> /dev/null; then
    echo "Installing ShellCheck..."
    # Defaulting to ShellCheck v0.10.0 (override with SCVERSION env var if needed)
    scversion="${SCVERSION:-v0.10.0}"
    wget -qO- "https://github.com/koalaman/shellcheck/releases/download/${scversion}/shellcheck-${scversion}.linux.x86_64.tar.xz" | tar -xJv
    sudo cp "shellcheck-${scversion}/shellcheck" /usr/local/bin/
    rm -rf "shellcheck-${scversion}"
else
    echo "ShellCheck is already installed."
fi

# 5. Verify Java
if ! command -v java &> /dev/null; then
    echo "Error: java is not installed."
    exit 1
fi
echo "Java is available."

# 6. Verify Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME is not set."
    echo "Please install the Android SDK and Command Line Tools."
    echo "Set ANDROID_HOME to your SDK location."
    exit 1
fi
echo "ANDROID_HOME is set: $ANDROID_HOME"

# 7. Verify AWS CLI (required for infrastructure audit)
if ! command -v aws &> /dev/null; then
    echo "Warning: AWS CLI is not installed. Infrastructure audit (Tier 4) will fail."
    echo "Install from: https://aws.amazon.com/cli/"
else
    echo "AWS CLI is available: $(aws --version)"
fi

echo "CI Environment Setup Complete."
