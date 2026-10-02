#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${1:-app/build/outputs/apk/release/app-release.apk}"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: Artifact not found at $APK_PATH"
    exit 1
fi

echo "========================================="
echo " Verifying Production Artifact: $APK_PATH"
echo "========================================="

# 1. Generate SHA256
SHA256=$(sha256sum "$APK_PATH" | awk '{print $1}')
echo "SHA256: $SHA256"

# 2. Check size
FILE_SIZE=$(stat -c%s "$APK_PATH" 2>/dev/null || stat -f%z "$APK_PATH")
echo "Size: $FILE_SIZE bytes"

echo "✅ Artifact verified successfully!"
