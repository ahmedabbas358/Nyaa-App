# AniFlow — UX, Interaction & Design Rules

> **Enforcing STEP 15 Section 14, 15, 61–70, 73–80 & 107–111.**

---

## 1. The 3-Second Comprehension Rule (Section 111)

Every screen in AniFlow must be immediately comprehensible within **2 to 3 seconds**. If a screen requires deciphering dense tables, ambiguous abbreviations, or cluttered buttons, it has violated our UX standards:
- **Prioritize**: Surface primary metadata first; relegate secondary diagnostic data to collapsible sections.
- **De-clutter**: Avoid showing 10 badges on a single card.
- **Guide**: When an error or empty state occurs, immediately provide a one-tap solution.

---

## 2. Information Hierarchy for Release Cards (Section 14 & 15)

Release cards strictly follow an 8-level information hierarchy:

```text
┌─────────────────────────────────────────────────────────────────┐
│ 1. Canonical Title [SubsPlease] One Piece                      │
│ 2. Episode / Range: Episode 1089                                │
│ ─────────────────────────────────────────────────────────────── │
│ [1080p] [HEVC] [Dual Audio] [Batch]            ← 3, 4, 5 Badges │
│ ─────────────────────────────────────────────────────────────── │
│ 6. Size: 1.4 GiB  •  7. Uploader: Erai-raws  •  8. Seeds: 240   │
└─────────────────────────────────────────────────────────────────┘
```

### Badge Rules
Only high-impact technical decision factors earn badges:
- **Quality**: `1080p`, `720p`, `4K`
- **Encoding**: `HEVC`, `AV1`
- **Audio**: `Dual Audio`, `Multi-Audio`
- **Format**: `Batch`
- **Status**: `Preferred`, `Fallback`, `Warning`  
*(Secondary details like CRF, bitrate, and encoder tool belong in the Release Details sheet).*

---

## 3. Interaction & Feedback Patterns (Section 108)

AniFlow forbids full-screen blocking spinners for interactive flows. We adhere to a predictable 5-stage interaction pattern:

```text
User Tap 
   ↓ (Within 50ms)
Immediate Visual Feedback (Ripple, button state change, skeleton loader)
   ↓ (Asynchronous)
Background Execution on IO Coroutine Dispatcher
   ↓ (Streaming)
Progress Updates (Throttled progress bar / ETA)
   ↓
Result Display with Smooth In-place State Transition
```

---

## 4. Confirmation Dialogs vs Undo Snackbars (Section 67 & 70)

### High-Risk Actions (Modal Confirmation Dialog Required)
- Deleting an anime file from disk permanently.
- Overwriting an existing media file during an upgrade.
- Clearing the entire download history or queue.
- Resetting application settings to factory defaults.

### Reversible / Low-Risk Actions (Zero Dialog — Undo Snackbar Only)
- Pausing or resuming a download task.
- Adding or removing an anime from Favorites.
- Ignoring a release or uploader.
- Removing an item from a collection.
*(A bottom Snackbar with a 5-second `Undo` action appears; no blocking dialog).*

---

## 5. Empty States & First-Run Guidance (Section 61 & 62)

Empty states must never display generic "No data" messages. They must be active and instructive:
- **Empty Search**: *"Search for any anime title (e.g. 'One Piece' or 'Frieren') to discover releases."*
- **Empty Downloads**: *"Your download queue is empty. Find an anime in Search or check your Library."*
- **Empty Library**: *"No media indexed yet. Downloaded anime will automatically organize here."*

---

## 6. Color Semantics & Dark Theme (Section 74 & 78)

- **Deep Charcoal Surfaces**: Avoid pure pitch black (`#000000`) for surfaces to prevent OLED smearing. Use curated dark tones (`#121212`, `#1E1E1E`, `#252525`).
- **Semantic Colors**:
  - `Success` (`#4CAF50`): Completed downloads, high-confidence parses, verified checksums.
  - `Warning` (`#FF9800`): Ambiguous title matches, degraded provider health, partial batches.
  - `Error` (`#F44336`): Download failures, storage full, invalid paths.
  - `Preferred` (`#2196F3`): Releases matching user's active download profile.
  - `Neutral` (`#9E9E9E`): Unmatched secondary releases, metadata timestamps.

---

## 7. RTL & Bidirectional Text Rules (Section 60 & 77)

1. In Arabic right-to-left layouts, **never** allow technical strings like `1080p`, `HEVC`, `[Group]`, or `123–128` to break or invert punctuation.
2. All technical strings must pass through `BidiFormatter.isolateLtr()` to be wrapped in Unicode Left-to-Right Isolates (`\u2066` and `\u2069`).
3. Numerical episode ranges (`01–12`) and file sizes (`1.4 GiB`) must maintain proper left-to-right orientation.
