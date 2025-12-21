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
pip install -r scripts/requirements.txt
echo "Dependencies installed."

# 3. Verify Trufflehog
# According to memory, we use a 'Verify & Fail' strategy for trufflehog, requiring it to be pre-installed.
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
