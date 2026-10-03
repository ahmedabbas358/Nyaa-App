#!/usr/bin/env bash
set -euo pipefail

echo "========================================="
echo " Building AniFlow Production Artifacts   "
echo "========================================="

if [ -z "${KEYSTORE_FILE:-}" ]; then
    echo "⚠️ Warning: KEYSTORE_FILE is not set. Building with debug fallback key."
fi

./gradlew clean assembleRelease bundleRelease --stacktrace

echo "📦 Artifacts generated in app/build/outputs/"
ls -lh app/build/outputs/apk/release/*.apk
ls -lh app/build/outputs/bundle/release/*.aab
echo "✅ Build completed successfully!"
