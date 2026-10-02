#!/usr/bin/env bash
set -euo pipefail

echo "========================================="
echo " AniFlow Static Analysis & Architecture"
echo "========================================="

echo "1. Running Android Lint..."
./gradlew lintDebug

echo "2. Running Architecture Verification Tests..."
./gradlew :testing:test --tests "com.aniflow.testing.ArchitectureVerificationTests"

echo "✅ All static analysis and architecture checks passed!"
