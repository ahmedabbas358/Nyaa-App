# AniFlow Production Quality Gates

Enforcing STEP 13 Section 1 (Production Quality Gates) and Section 90 (Release Checklist).

No Production Release APK/AAB may be signed or distributed if any primary quality gate fails.

---

## 1. The 10 Production Quality Gates

| Gate | Name | Automated Verification Tool | Failure Policy |
| :--- | :--- | :--- | :--- |
| **Gate 1** | **Build Matrix** | `gradlew assembleDebug assembleRelease` | Strict Blocker |
| **Gate 2** | **Static Analysis** | `detekt`, `Android Lint`, `ktlint` | Zero Fatal / Warning threshold |
| **Gate 3** | **Unit Tests** | `gradlew test` (Domain & Core) | 100% Pass rate required |
| **Gate 4** | **Integration Tests** | E2E Integration Suite (Room, Provider, Cache) | Strict Blocker |
| **Gate 5** | **UI & Accessibility** | Compose tests, TalkBack, Bidi RTL validation | Blocker on P0/P1 issues |
| **Gate 6** | **End-to-End Tests** | 14-Step Full Lifecycle Acceptance Test | Strict Blocker |
| **Gate 7** | **Performance & Memory** | Startup latency baseline, LeakCanary, DB Index checks | Blocker if startup > 2.5s |
| **Gate 8** | **Reliability & Recovery** | Chaos failure injection, Process death recovery | Zero data corruption tolerated |
| **Gate 9** | **Security & Privacy** | Path traversal suite, Secret redaction, Export checks | Zero secret leakage allowed |
| **Gate 10** | **Release Validation** | R8 minification, ProGuard mapping, Schema checks | Strict Blocker |

---

## 2. Bug Severity Classification (Section 89)

- **P0 — Critical / Fatal**: Data loss, unrecoverable crash loop, database corruption, security leakage, path traversal. *Blocks all builds immediately.*
- **P1 — Blocker**: Primary feature broken (Search, Download execution, File moving, Automation) with no workaround. *Blocks Release Candidate.*
- **P2 — Major**: Important feature issue with viable fallback or workaround.
- **P3 — Minor**: Non-blocking edge case or minor UI inconsistency.
- **P4 — Trivial**: Minor styling, padding or cosmetic adjustment.

---

## 3. Strict Pre-Release Verification Checklist (Section 90)

- [x] All 10 Quality Gates configured and audited.
- [x] Clean architecture boundaries enforced (Domain independent of Android/Room/OkHttp).
- [x] R8 and ProGuard keep rules tailored without wildcards.
- [x] Path traversal security enforced by `PathSanitizer`.
- [x] Log redactor removes all tokens, passwords, and tracker credentials.
- [x] Download state machine strictly rejects illegal transitions.
- [x] Storage reservation prevents concurrent out-of-space allocations.
- [x] Progress persistence throttler protects SQLite from write floods.
- [x] Automation loop protector blocks recursive triggers and duplicate events.
- [x] Full Arabic RTL and English localization supported.
