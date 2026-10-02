# AniFlow Production Release Process

This document describes the step-by-step release process for AniFlow, adhering to STEP 14 specifications.

---

## 1. Release Lifecycle Overview

```text
Feature Freeze
      ↓
Release Candidate (RC) Branch / Tag
      ↓
Automated Pre-Release Verification (CI)
      ↓
Sign Artifacts (APK / AAB)
      ↓
Artifact Integrity Validation & Manifest
      ↓
Internal Dogfooding / Staged Rollout
      ↓
Production Distribution
      ↓
Telemetry Monitoring
      ↓
Patch / Hotfix (if required)
```

---

## 2. Versioning Specification (Section 4 & 5)

We enforce semantic versioning (`MAJOR.MINOR.PATCH`):
- `versionName`: e.g. `1.0.0`
- `versionCode`: e.g. `100` (computed as `MAJOR * 10000 + MINOR * 100 + PATCH`)
- `dbVersion`: Managed independently in Room migration schemas (`AniFlowDatabase`).
- `parserVersion`: Managed independently to trigger re-parsing of cached provider releases.

---

## 3. Signing Key Management (Section 9, 10 & 11)

- **Debug Builds**: Sign automatically with standard debug keystore.
- **Production Builds**: Keystore material is injected securely via CI secrets or environment variables:
  - `KEYSTORE_FILE`: Absolute path to the release `.keystore` file.
  - `KEYSTORE_PASSWORD`: Keystore access password.
  - `KEY_ALIAS`: Private key alias.
  - `KEY_PASSWORD`: Private key password.
- **NEVER** commit keystore files or passwords to Git.

---

## 4. Staged Rollout Strategy (Section 39)

1. **Internal Channel (100% of internal team)**: Smoke testing, recovery testing.
2. **Phase 1 (10% rollout)**: Monitor Crash Rate, ANR Rate, and Provider circuit breaker health for 48 hours.
3. **Phase 2 (50% rollout)**: Monitor database migration success and download completion rates.
4. **Phase 3 (100% full rollout)**.

---

## 5. Rollback & Hotfix Plan (Section 40 & 74)

If a critical P0/P1 defect is discovered during rollout:
1. **Halt Rollout immediately** in the distribution console.
2. **Reproduce and isolate root cause** using the developer diagnostics report.
3. **Prepare minimal patch branch** (`hotfix/1.0.1`) targeting the release commit.
4. **Validate database schema backward compatibility** (never perform destructive schema rollbacks).
5. **Deploy hotfix release** incrementing PATCH version.
