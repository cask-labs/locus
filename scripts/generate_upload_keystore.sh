#!/bin/bash
set -e

# Setup Output Directory
OUTPUT_DIR="keystore_output"
mkdir -p "$OUTPUT_DIR"
KEYSTORE_PATH="$OUTPUT_DIR/upload.jks"

echo "=========================================="
echo "   Locus Release Keystore Generator"
echo "=========================================="
echo "This script will generate a new Java Keystore (JKS) for signing Android releases."
echo "It will then output the values you need to set in GitHub Actions Secrets."
echo ""

# Input prompts
read -r -p "Enter Key Alias (default: locus-upload): " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-locus-upload}

read -r -s -p "Enter Key Store Password (min 6 chars): " STORE_PASSWORD
echo ""
read -r -s -p "Enter Key Password (min 6 chars, press enter to use same as store): " KEY_PASSWORD
echo ""
KEY_PASSWORD=${KEY_PASSWORD:-$STORE_PASSWORD}

echo ""
echo "Generating keystore at $KEYSTORE_PATH..."

# Generate Keystore
# Validity 10000 days (~27 years)
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=Locus Developer, OU=Mobile, O=Locus, L=Unknown, S=Unknown, C=Unknown"

echo ""
echo "=========================================="
echo "SUCCESS! Keystore generated."
echo "=========================================="
echo ""
echo "Please go to your GitHub Repository -> Settings -> Secrets and variables -> Actions"
echo "And create the following Repository Secrets:"
echo ""

# Base64 Encode
# Use strict mode (no newlines) if possible, but the gradle script now handles MIME/newlines.
# We will use 'base64' command. Linux 'base64 -w 0' disables wrapping. Mac 'base64' is fine but might wrap.
# To be safe for cross-platform and the MimeDecoder, we can output standard base64.
BASE64_KEYSTORE=$(base64 < "$KEYSTORE_PATH")

echo "1. LOCUS_UPLOAD_KEYSTORE_BASE64"
echo "   (Copy the content between the markers below)"
echo "------------------------------------------"
echo "$BASE64_KEYSTORE"
echo "------------------------------------------"
echo ""
echo "2. LOCUS_KEY_ALIAS"
echo "   $KEY_ALIAS"
echo ""
echo "3. LOCUS_STORE_PASSWORD"
echo "   $STORE_PASSWORD"
echo ""
echo "4. LOCUS_KEY_PASSWORD"
echo "   $KEY_PASSWORD"
echo ""
echo "IMPORTANT: Save the file '$KEYSTORE_PATH' in a secure location (e.g., 1Password, LastPass)."
echo "Do NOT check it into the git repository."
echo "=========================================="
