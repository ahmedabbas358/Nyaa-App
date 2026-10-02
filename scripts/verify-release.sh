#!/usr/bin/env bash
# Bash Release Verification Script (Sections 61 & 101)
set -e

EXPECTED_VERSION="${1:-1.0.0}"
echo "=== Starting AniFlow Release Verification (v${EXPECTED_VERSION}) ==="

HAS_ERRORS=0

# 1. Version Consistency Check
FOUND_VERSION=$(grep -o 'versionName\s*=\s*"[^"]*"' app/build.gradle.kts | cut -d'"' -f2)
if [ "$FOUND_VERSION" != "$EXPECTED_VERSION" ]; then
    echo "[FAIL] Version mismatch: app/build.gradle.kts has '$FOUND_VERSION' but expected '$EXPECTED_VERSION'"
    HAS_ERRORS=1
else
    echo "[PASS] Version in app/build.gradle.kts matches '$EXPECTED_VERSION'"
fi

# 2. Check CHANGELOG.md contains version
if grep -q "##.*$EXPECTED_VERSION" CHANGELOG.md; then
    echo "[PASS] Version '$EXPECTED_VERSION' documented in CHANGELOG.md"
else
    echo "[FAIL] Version '$EXPECTED_VERSION' missing from CHANGELOG.md"
    HAS_ERRORS=1
fi

# 3. Secret Pattern Check
if grep -r --exclude-dir={build,.gradle,.git,scripts} -i "BEGIN .*PRIVATE KEY" .; then
    echo "[CRITICAL FAIL] Private key pattern detected!"
    HAS_ERRORS=1
fi

# 4. Mandatory Files Check
REQUIRED_FILES=("README.md" "LICENSE" "SECURITY.md" "CONTRIBUTING.md" "docs/release.md" ".gitignore")
for req in "${REQUIRED_FILES[@]}"; do
    if [ -f "$req" ]; then
        echo "[PASS] Required file exists: $req"
    else
        echo "[FAIL] Missing required file: $req"
        HAS_ERRORS=1
    fi
done

if [ $HAS_ERRORS -eq 1 ]; then
    echo "=== Release Verification FAILED ==="
    exit 1
else
    echo "=== Release Verification PASSED Successfully ==="
    exit 0
fi
