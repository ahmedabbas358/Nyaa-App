# AniFlow — Intentional Product Limits & Guardrails

> **Enforcing STEP 15 Section 97 (Product Limits).**  
> *All limits are intentional, configurable where appropriate, and protected by automated tests.*

---

## 1. Concurrency & Network Limits

| Parameter | Default Value | Configurable Range | Rationale / Enforcement |
| :--- | :---: | :---: | :--- |
| **Max Concurrent Downloads** | `3` | `1` to `8` | Prevents disk thrashing, network bufferbloat, and thermal throttling. |
| **Max HTTP Segments per File**| `4` | `1` to `8` | Prevents server-side rate limits or IP bans from indexing providers. |
| **Provider Min Request Interval**| `500 ms` | Fixed | Enforced by `RequestLimiter` to protect external provider servers. |
| **Circuit Breaker Failure Threshold**| `4 failures` | Fixed | Enforced by `ProviderCircuitBreaker` before opening circuit. |
| **Circuit Breaker Open Cooldown**| `30 seconds` | Fixed | Duration before allowing half-open probe request. |

---

## 2. Storage & Filesystem Limits

| Parameter | Default Value | Configurable Range | Rationale / Enforcement |
| :--- | :---: | :---: | :--- |
| **Storage Safety Margin** | `500 MB` | `200 MB` to `2 GB` | Enforced by `StorageReservationManager` to ensure OS stability. |
| **Max Filename Length** | `240 bytes` | Fixed | Enforced by `PathSanitizer` to comply with ext4, F2FS, and NTFS limits. |
| **Path Traversal Escape** | `STRICTLY 0` | Non-configurable | `PathSanitizer.ensureInsideStorageRoot` throws `SecurityException`. |
| **Progress DB Persistence Rate**| `1000 ms` / `5%` | Fixed | Enforced by `ProgressPersistenceThrottler` to prevent SQLite write flood. |

---

## 3. Control Plane & Automation Limits

| Parameter | Default Value | Configurable Range | Rationale / Enforcement |
| :--- | :---: | :---: | :--- |
| **Max Automation Recursion Depth**| `3 levels` | Non-configurable | Enforced by `AutomationLoopProtector` to stop runaway upgrade loops. |
| **Item Automation Cooldown** | `60 seconds` | Fixed | Suppresses duplicate automated triggers on identical target items. |
| **Max Saved Searches** | `50` | `10` to `100` | Prevents excessive background battery consumption. |
| **Max Automation Rules** | `100` | Fixed | Guarantees sub-millisecond rule evaluation times. |
| **Max Import Config File Size**| `5 MB` | Fixed | Enforced by `ConfigurationSecurity` against memory exhaustion attacks. |
| **Max Rule AST Nesting Depth** | `10 levels` | Fixed | Enforced by `ConfigurationSecurity` against StackOverflowError. |

---

## 4. Cache & Retention Limits

| Parameter | Default Value | Configurable Range | Rationale / Enforcement |
| :--- | :---: | :---: | :--- |
| **Max Download History Records**| `500 records`| `100` to `2000` | Automatic FIFO pruning of oldest completed/cancelled records. |
| **Provider Search Cache TTL** | `24 hours` | `1 hour` to `7 days`| Stale search results auto-expire to maintain fresh seeder stats. |
| **Offline Crash Log Queue** | `20 events` | Fixed | Enforced by `CrashReportingService` with sensitive data redaction. |
