# AniFlow Release Operations & CI Configuration Guide

This document describes the procedures for building, signing, verifying, and publishing official releases of AniFlow, both locally and through GitHub Actions CI/CD.

---

## 1. Prerequisites

- **Java Development Kit (JDK)**: OpenJDK 17 (e.g. Eclipse Temurin 17)
- **Android SDK**: Build Tools `35.0.0`, Platform SDK `35`, Min SDK `26`
- **Gradle**: Wrapper provided (Gradle 8.9+)
- **Git**: Git 2.30+

---

## 2. GitHub Secrets Configuration (CI Signing)

To enable automated release builds via the GitHub Actions Release Pipeline (`.github/workflows/release.yml`), configure the following **Repository Secrets** under `Settings > Secrets and variables > Actions`:

| Secret Name | Description | Example / Format |
| :--- | :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded production Android Keystore (`.jks` / `.keystore`) | Output of `base64 -w 0 aniflow.keystore` |
| `RELEASE_KEYSTORE_PASSWORD` | Master password for the keystore file | Alphanumeric passphrase |
| `RELEASE_KEY_ALIAS` | Private key alias inside the keystore | e.g. `aniflow-release-key` |
| `RELEASE_KEY_PASSWORD` | Password for the specified private key alias | Alphanumeric passphrase |

> [!CAUTION]
> **CRITICAL SECURITY RULE**: Never commit `.keystore`, `.jks`, or plain-text passwords into the Git repository.

---

## 3. Local Release Signing

To assemble a release build locally on a workstation:

1. Place your private keystore outside the Git workspace or in a gitignored path.
2. Export the environment variables in your terminal:
   ```bash
   export KEYSTORE_FILE="/path/to/aniflow.keystore"
   export KEYSTORE_PASSWORD="your-keystore-password"
   export KEY_ALIAS="your-key-alias"
   export KEY_PASSWORD="your-key-password"
   ```
   *(On Windows PowerShell)*:
   ```powershell
   $env:KEYSTORE_FILE="C:\SecureKeys\aniflow.keystore"
   $env:KEYSTORE_PASSWORD="your-keystore-password"
   $env:KEY_ALIAS="your-key-alias"
   $env:KEY_PASSWORD="your-key-password"
   ```
3. Run the release assemble task:
   ```bash
   ./gradlew assembleRelease bundleRelease
   ```
4. Output artifacts are located at:
   - APK: `app/build/outputs/apk/release/app-release.apk`
   - AAB: `app/build/outputs/bundle/release/app-release.aab`

---

## 4. Release Lifecycle & Verification Steps

### Step 4.1: Version Bump
Update the version identifiers across the repository:
1. `app/build.gradle.kts`:
   - `versionCode`: Increment monotonically (e.g. `100 -> 101`)
   - `versionName`: Semantic versioning (e.g. `1.0.0 -> 1.0.1`)
2. `CHANGELOG.md`: Record new features, fixes, and changes under the new version header.

### Step 4.2: Automated Pre-Release Verification
Run all quality gates prior to tagging:
```bash
./scripts/check.sh       # or ./scripts/check.ps1
./scripts/test.sh        # or ./scripts/test.ps1
```

### Step 4.3: Git Tagging & Publish
```bash
git status
git add .
git commit -m "release: prepare v1.0.0"
git tag -a v1.0.0 -m "AniFlow v1.0.0 Official Release"
git push origin main
git push origin v1.0.0
```

### Step 4.4: Checksum Generation & Verification
Compute SHA-256 hashes for all binaries:
```bash
sha256sum AniFlow-v1.0.0.apk > AniFlow-v1.0.0.apk.sha256
sha256sum AniFlow-v1.0.0.aab > AniFlow-v1.0.0.aab.sha256
```
To verify on target systems:
```bash
sha256sum -c AniFlow-v1.0.0.apk.sha256
```

---

## 5. Rollback Strategy
If an unforeseen critical defect occurs in production:
1. Immediately trigger the circuit breaker / rate-limiter fallback if provider-related.
2. In GitHub, mark the problematic release as a pre-release or add a prominent warning.
3. Prepare a hotfix branch (`fix/hotfix-v1.0.1`), increment `versionCode` and `versionName` to `1.0.1`, apply the patch, and trigger a patch release.
4. Never force-delete or overwrite existing published Git tags.
