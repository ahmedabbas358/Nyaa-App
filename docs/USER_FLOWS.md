# AniFlow — End-to-End User Flows & Journeys

> **Enforcing STEP 15 Section 2, 87–90, 116 & 117.**

---

## 1. Flow A: Search, Discover & Download (The Primary Flow)

```text
User enters search term (e.g. "Frieren")
       ↓
SearchCoordinator queries Nyaa Provider via HTTPS
       ↓
NyaaHtmlSearchParser extracts raw release rows
       ↓
ReleaseParser parses Title, Quality (1080p), Codec (HEVC), Audio & Subtitles
       ↓
ReleaseGroupingService aggregates results into Seasons & Episode maps
       ↓
UI displays structured Release Cards with clear badges and confidence score
       ↓
User selects release & taps "Download"
       ↓
PrepareDownloadPlanUseCase checks available disk space & duplicate status
       ↓
ExecuteDownloadPlanUseCase queues task in DownloadQueue
       ↓
Foreground Service starts download using chunked HTTP or Torrent engine
       ↓
File verified upon completion & atomically moved to target folder
       ↓
Indexed into Library for immediate offline access
```

---

## 2. Flow B: Episode-Centric Discovery & Comparison

```text
User navigates to an Anime page (e.g. "Chainsaw Man")
       ↓
Views Season 1 coverage grid (e.g. Episodes 01 to 12)
       ↓
Taps Episode 04 (marked as Missing "—")
       ↓
Screen displays Candidate Releases specifically for Episode 04
       ↓
User taps "Compare" between Release A (SubsPlease 1080p) and Release B (Judas 1080p HEVC)
       ↓
Side-by-Side comparison displays differences (Size, Codec, Audio channels, Seeds)
       ↓
User chooses Release B
       ↓
Download Task created specifically targeting Episode 04
```

---

## 3. Flow C: Batch Optimization vs Individual Episodes

```text
User requests "Download Season 1" for an anime with 12 missing episodes
       ↓
System queries provider and discovers:
  - 12 individual episode releases (Total size: 16.8 GB)
  - 1 complete batch release "[Group] Anime 01-12 [1080p]" (Total size: 12.2 GB)
       ↓
Season Optimization Calculator compares:
  - Bandwidth savings: Batch saves 4.6 GB
  - Consistency: Single release group and uniform subtitle styling
  - Storage reservation: 1 atomic task vs 12 concurrent queue slots
       ↓
UI presents recommendation:
  "Recommended: 1 Batch Release (12.2 GB, saves 4.6 GB)"
       ↓
User confirms; 1 unified Batch DownloadTask is created
       ↓
Files organized cleanly into Season 01 directory structure upon completion
```

---

## 4. Flow D: Saved Search & Continuous Automation

```text
User sets up Saved Search for an airing anime: "Jujutsu Kaisen Season 2"
       ↓
Attaches Rule: "If Resolution == 1080p AND ReleaseGroup == 'SubsPlease' -> Auto-Download"
       ↓
Periodic background worker queries Nyaa for new releases
       ↓
New release detected: "[SubsPlease] Jujutsu Kaisen - 45 (1080p).mkv"
       ↓
AutomationEngine passes release to AutomationLoopProtector:
  ✓ Cooldown validated (no recent duplicate trigger)
  ✓ Concurrency lock acquired
  ✓ Recursion depth = 1 (safe)
       ↓
RuleTreeEvaluator matches criteria -> triggers Auto-Download Action
       ↓
Pre-flight check verifies available storage and network policy (Wi-Fi only)
       ↓
Task queued automatically; Silent notification posted: "New episode downloading: Episode 45"
```

---

## 5. Flow E: Safe Quality Upgrade Workflow

```text
User library contains Episode 01 in 720p H.264 (800 MB)
       ↓
Provider discovery identifies a new release: 1080p BDRip HEVC Dual Audio (1.4 GB)
       ↓
UpgradePolicyRule evaluates upgrade:
  ✓ Resolution improved (720p -> 1080p)
  ✓ Codec improved (H.264 -> HEVC)
  ✓ Audio improved (Single -> Dual Audio)
       ↓
Download Task scheduled for the 1080p version
       ↓
1080p download completes & passes hash/checksum verification
       ↓
Safe replacement executes:
  - 1080p file moved into library
  - Old 720p file handled according to user policy:
    (Default: Quarantined to Trash / Kept until user confirmation)
       ↓
Library metadata updated; UI reflects upgraded 1080p badge
```

---

## 6. Flow F: Sudden Process Death Recovery

```text
Download Task is actively downloading at 65% (file.part exists on disk)
       ↓
Device runs out of battery or OS kills background process
       ↓
User charges device and reopens AniFlow
       ↓
Application startup initializes SafeRepairService:
       ↓
Detects tasks stuck in transient 'Downloading' state without active worker
       ↓
Reconciles task state back to 'Queued'
       ↓
PartialFileIntegrityManager inspects 'file.part':
  ✓ File exists on disk
  ✓ Length (650 MB) <= expected total (1000 MB)
  ✓ Provider supports HTTP Range headers
  → Assessed as: ResumeEligible
       ↓
Scheduler resumes download starting at byte offset 681,574,400 (65%)
       ↓
Zero corrupted bytes, zero re-download waste
```

---

## 7. User Stories Acceptance Matrix (Section 117)

| User Story | Implementation & Engine | Verified |
| :--- | :--- | :---: |
| *Search for an anime* | `SearchReleasesCoordinatorUseCase` + Nyaa Provider | ✅ |
| *Search by uploader* | `ProviderSearchRequest.uploader` mode | ✅ |
| *Group releases by group/uploader* | `ReleaseGroupingService` | ✅ |
| *Compare multiple releases side-by-side* | Release comparison screen + `ReleaseComparisonModel` | ✅ |
| *Select preferred uploader & codec* | `DownloadProfile` + `PreferenceResolver` | ✅ |
| *Download an entire batch release* | `ReleaseType.Batch` handling in `PrepareDownloadPlanUseCase` | ✅ |
| *Download only missing episodes* | `EpisodeCoverageMap` + missing filter | ✅ |
| *Automate future release downloads* | `AutomationEngine` + `SavedSearch` | ✅ |
| *Prevent duplicate downloads* | `DuplicateDetector` + `AutomationLoopProtector` | ✅ |
| *Replace old release safely* | `UpgradePolicyRule` + non-destructive quarantine | ✅ |
| *Choose where files are stored* | Scoped storage routing via SAF (`StorageTarget`) | ✅ |
| *Recover downloads after crash* | `SafeRepairService` + `PartialFileIntegrityManager` | ✅ |
| *Understand why a release was selected* | Human-readable reasons in `SelectionResult.explanation` | ✅ |
| *Override automated decisions manually* | Manual selection flag taking precedence over rules | ✅ |
| *Organize library into folders automatically* | `MediaOrganizationService` (Plex/Jellyfin naming) | ✅ |
