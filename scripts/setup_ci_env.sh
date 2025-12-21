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

# Fix for PyYAML 5.4.1 build failure on Python 3.10+ (AttributeError: cython_sources)
# This is caused by PyYAML 5.4.1 setup.py incompatibility with Cython 3.0.
# We must ensure Cython < 3.0 is used during the build.
# --no-build-isolation forces pip to use the installed packages (cython<3) instead of creating a fresh build env.
pip install "cython<3.0.0" "wheel"
pip install "pyyaml==5.4.1" --no-build-isolation

pip install -r scripts/requirements.txt
echo "Dependencies installed."

# 3. Verify Trufflehog
if ! command -v trufflehog &> /dev/null; then
    echo "Error: trufflehog is not installed. Please install trufflehog."
    exit 1
fi
echo "trufflehog is available."

# 4. Verify Java
if ! command -v java &> /dev/null; then
    echo "Error: java is not installed."
    exit 1
fi
echo "Java is available."

echo "CI Environment Setup Complete."
