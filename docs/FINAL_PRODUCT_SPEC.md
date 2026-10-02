# AniFlow — Final Product Specification (v1.0.0)

> **Source of Product Truth for AniFlow Android**  
> *Enforcing STEP 15 Section 1, 2, 87–90, 113–115, 119 & 125.*

---

## 1. Product Vision & Identity

AniFlow is an **Android Native** platform combining:
1. **Release Discovery & Search Engine** (specialized in anime releases from indexing providers such as Nyaa).
2. **Release Intelligence & Grouping Engine** (extracting resolution, codec, audio, subtitle, release group, episode ranges, and confidence scoring).
3. **Smart Selection & Ranking Engine** (rule-driven, profile-based, explainable selection with positive/negative rationale).
4. **Download Orchestrator & Dual Runtimes** (high-speed chunked HTTP engine + BitTorrent engine with priority queuing and atomic storage reservations).
5. **Storage & Media Library Manager** (Scoped storage via SAF, automatic naming templates, atomic moves, media verification, and missing episode tracking).
6. **Automation Control Plane** (declarative boolean rule AST, saved searches, loop protection, and upgrade workflows).

AniFlow is **not** merely a torrent client, a simple search scraper, or a generic file manager. It is a unified discovery-to-library media management lifecycle:

```text
Discover → Understand → Group → Compare → Select → Plan → Download → Organize → Track → Upgrade
```

---

## 2. Core User Promise

A user can fulfill their entire anime consumption journey from one screen without jumping across browser tabs, torrent clients, file renamers, or media scrapers:

```text
"I want an anime"
       ↓
Find releases across providers
       ↓
Understand exactly what each release contains (quality, codec, audio, subs)
       ↓
Compare uploaders, groups, and batch completeness
       ↓
Choose manually or let smart profiles select the optimal release
       ↓
Download safely with atomic storage checks and resume capability
       ↓
Organize files cleanly into Plex/Jellyfin/Kodi folder structures
       ↓
Track missing episodes on a season coverage map
       ↓
Automatically discover and upgrade new episodes when released
```

---

## 3. User Personas & Experience Tiers

AniFlow serves three user tiers without cluttering the basic experience:

### Tier 1: The Casual Anime Fan (Beginner)
- **Goal**: Search an anime title, pick a release, tap Download, watch when ready.
- **Experience**: Clean search bar, clear release cards, automatic safe defaults, simple download status, no technical jargon required.

### Tier 2: The Enthusiast Collector (Advanced)
- **Goal**: Collect complete seasons in 1080p HEVC Dual Audio from reputable groups, download batches, avoid missing episodes.
- **Experience**: Download profiles, batch vs episode comparison, season coverage maps, preferred uploader ranking, custom folder templates.

### Tier 3: The Power User & Archivist (Pro)
- **Goal**: Automated tracking of ongoing series, AST rule builder, bandwidth scheduling, storage routing to external SD cards/USB drives, diagnostics export.
- **Experience**: Visual AST condition tree, fallback tiers, circuit breaker controls, offline crash telemetry, detailed peer and segment metrics.

---

## 4. Architectural Philosophy

AniFlow adheres strictly to **Clean Architecture** with unidirectional data flow (UDF):
- **`:presentation`**: Jetpack Compose, Material 3, ViewModels, UI state machines. (Zero access to DAOs or raw network).
- **`:domain`**: Pure Kotlin/JVM. Contains all business models, value objects, state machines, use cases, and algorithms. (Zero Android SDK, Compose, or Room dependencies).
- **`:data` / `:core` / `:provider` / `:download`**: Infrastructure implementations (Room, OkHttp, Jsoup, SAF storage, foreground services).

### The Guiding Product Principle (Section 125)
> **POWERFUL UNDER THE HOOD. SIMPLE ON THE SURFACE. EXPLAINABLE BY DEFAULT. AUTOMATIC ONLY WHEN USER INTENDS IT. SAFE WHEN UNCERTAIN.**

---

## 5. Core Subsystem Specifications

### 5.1 Discovery & Search Engine
- **Provider Abstraction**: Decoupled from Nyaa-specific schemas; provider capabilities declared upfront (`search`, `sorting`, `filtering`, `uploaderSearch`, `details`, `torrent`, `magnet`).
- **Search Modes**: General, Anime, Uploader, Exact Title, Episode, Batch, and Season.
- **Query Compilation**: Visual search filters compiled to structured AST expressions before translation to provider-specific queries.

### 5.2 Release Intelligence & Title Parsing
- **Pipeline**: Tokenizer → Normalizer → Extractors (Resolution, Codec, Audio, Subtitles, Episode Range, Version, Batch, Movie).
- **Confidence Scoring**: High, Medium, Low, Ambiguous.
- **Ambiguity Rule**: If episode or title cannot be verified with certainty, it is flagged as `Review Required`. **Never guess.**

### 5.3 Smart Selection & "Why" Layer
- **Strict Precedence**: Hard Eligibility Constraints → Preferences → Weighted Score → Deterministic Tie-Breaker → Explanation.
- **Explainability**: Every automatic selection generates human-readable reasons (e.g. `✓ 1080p required`, `✓ HEVC preferred`, `✓ SubsPlease preferred uploader`, `✗ 500 MB larger`).

### 5.4 Download Planning & State Machine
- **Atomic Space Reservation**: Prevents overallocating disk space when multiple concurrent tasks start.
- **Strict State Machine**: `Pending → Queued → Starting → Downloading → Verifying → Organizing → Completed`. Illegal transitions (such as `Completed → Downloading` or skipping verification) are strictly rejected.
- **Partial File Safety**: `.part` and `.tmp` files tracked per task ID; verified before resuming.

### 5.5 Storage, SAF & Media Organization
- **Scoped Storage**: Uses Android Storage Access Framework (SAF) with persistent tree permissions.
- **Path Traversal Protection**: Enforced via `PathSanitizer.ensureInsideStorageRoot`; forbids `..`, null bytes, and reserved device names (`CON`, `PRN`, `AUX`, `NUL`).
- **Template Naming**: Supports standard presets (`Plex`, `Jellyfin`, `Kodi`, `Simple`, `Custom`).

### 5.6 Control Plane & Automation
- **Loop Protection**: Cooldown per target item (60s), recursion depth ceiling (`MAX_DEPTH = 3`), in-flight concurrency locks, and trigger deduplication.
- **Safe Defaults**: Auto-delete replaced files = **OFF by default**. Auto-download = **OFF by default**.

---

## 6. Security, Privacy & Reliability Guardrails

1. **Untrusted Data Model**: All provider inputs (titles, uploaders, filenames, URLs) are treated as untrusted.
2. **Secrets Protection**: Keystores, passwords, and tokens are strictly banned from Git and redacted from logs via `LogRedactor`.
3. **Zero Telemetry**: No external analytics, tracking, or telemetry servers. All data remains 100% on the local device.
4. **Crash Recovery**: Stuck transient download states are reconciled automatically back to `Queued` on app restart via `SafeRepairService`.

---

## 7. Localization & Accessibility

- **Languages**: English and Arabic out of the box with zero hardcoded user strings.
- **Bidirectional Isolation**: All technical tokens (`1080p`, `HEVC`, `[Group]`, `123–128`) use Unicode directional markers (`BidiFormatter`) to prevent punctuation flipping in RTL layouts.
- **Accessibility**: Semantic content descriptions, minimum 48dp touch targets, dynamic font scaling support.
