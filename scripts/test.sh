#!/usr/bin/env bash
set -euo pipefail

echo "========================================="
echo " Running AniFlow Automated Test Suites   "
echo "========================================="

./gradlew test --stacktrace

echo "✅ All tests passed successfully!"
