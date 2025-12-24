#!/bin/bash
set -e

echo "Starting Jules Environment Setup..."

# --- Configuration ---
ANDROID_SDK_ROOT="$HOME/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
TRUFFLEHOG_VERSION="v3.92.4"
TRUFFLEHOG_URL="https://github.com/trufflesecurity/trufflehog/releases/download/${TRUFFLEHOG_VERSION}/trufflehog_${TRUFFLEHOG_VERSION:1}_linux_amd64.tar.gz"
BIN_DIR="$HOME/bin"

# Create directories
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
mkdir -p "$BIN_DIR"

# Add bin to PATH for this session
export PATH="$BIN_DIR:$PATH"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# --- 1. Android SDK Setup ---
if [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
    echo "Downloading Android Command Line Tools..."
    curl -L -o cmdline-tools.zip "$CMDLINE_TOOLS_URL"
    unzip -q cmdline-tools.zip
    # Move to correct structure: cmdline-tools/latest/bin
    mv cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    rm cmdline-tools.zip
    echo "Android Command Line Tools installed."
else
    echo "Android Command Line Tools already installed."
fi

echo "Accepting Licenses..."
yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null

echo "Installing Android SDK Packages..."
"$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "Configuring local.properties..."
echo "sdk.dir=$ANDROID_SDK_ROOT" > /app/local.properties

# --- 2. TruffleHog Setup ---
if [ ! -f "$BIN_DIR/trufflehog" ]; then
    echo "Downloading TruffleHog $TRUFFLEHOG_VERSION..."
    curl -L -o trufflehog.tar.gz "$TRUFFLEHOG_URL"
    tar -xzf trufflehog.tar.gz -C "$BIN_DIR" trufflehog
    rm trufflehog.tar.gz
    chmod +x "$BIN_DIR/trufflehog"
    echo "TruffleHog installed."
else
    echo "TruffleHog already installed."
fi

# --- 3. Python Dependencies ---
echo "Installing Python dependencies..."
python3 -m pip install -r /app/scripts/requirements.txt

# --- 4. Gradle Warm-up ---
echo "Warming up Gradle dependencies..."
cd /app
./gradlew --refresh-dependencies --no-daemon

echo "Jules Environment Setup Complete!"
