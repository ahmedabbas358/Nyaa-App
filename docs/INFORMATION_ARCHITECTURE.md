# AniFlow — Information Architecture & Navigation Spec

> **Enforcing STEP 15 Section 114 & 115.**

---

## 1. Top-Level Navigation Shell

AniFlow uses a unified **5-Destination Bottom Navigation Bar** on compact devices (phones) and a persistent **Navigation Rail** on medium/expanded devices (tablets, foldables, and landscape mode):

```text
[ 🏠 Home ]   [ 🔍 Search ]   [ ⬇️ Downloads ]   [ 📚 Library ]   [ ⚙️ More ]
```

---

## 2. Detailed Navigation Tree

### 2.1 HOME (`/home`)
- **Search Header**: Tap opens the Search surface immediately with keyboard focused.
- **Active Download Pill/Card**: Real-time progress bar, speed, and ETA when tasks are downloading; hidden when queue is idle.
- **Missing Episodes Attention Card**: Highlights airing anime with missing episodes in the user's library.
- **Recently Added Media**: Horizontal carousel of newly indexed library files with quick play/open actions.
- **Saved Searches & Airing Watchlist**: Fast access to scheduled searches for active seasons.
- **Collections Row**: Fast shortcuts to user-defined collections (e.g. *Favorites*, *Watch Later*, *Archived*).

---

### 2.2 SEARCH (`/search`)
- **Query Input & Search Modes**:
  - General Anime Search
  - Uploader Search (`uploader:SubsPlease`)
  - Batch Only Toggle
- **Query Builder Toggle**: Switch between simple search and visual AST condition builder.
- **Quick Filters Bar**: Scrollable horizontal chips (`1080p`, `720p`, `HEVC`, `Dual Audio`, `Batch`, `Preferred Only`).
- **Advanced Filter Sheet**: Modal bottom sheet for Seeders, Size range, Date range, and Audio/Subtitle language.
- **Search Results List**:
  - **Flat Mode**: Chronological or seeder-sorted list.
  - **Grouped Mode**: Releases aggregated under canonical Anime title and Season.
- **Comparison Drawer**: Sliding bottom bar allowing users to select 2 to 4 releases and view side-by-side technical differences.

---

### 2.3 DOWNLOADS (`/downloads`)
- **Dashboard Banner**: Global download speed, total active slots, and aggregate ETA.
- **Global Actions**: `Pause All`, `Resume All`, `Retry Failed`, `Clear Completed`.
- **Status Tabs**:
  - `Active`: Currently streaming tasks with real-time transfer graphs.
  - `Queued`: Tasks waiting for network slots or scheduled time.
  - `Paused`: Manually suspended downloads.
  - `Failed`: Tasks stopped due to network timeout or storage limits with smart explanation.
  - `Completed`: Finished tasks with destination paths and verification badges.
- **Task Detail Sheet (`/downloads/{taskId}`)**:
  - *Overview Tab*: Title, source URL, state, progress, total size, ETA.
  - *Files Tab*: Multi-file selective checkboxes (for torrent/batch downloads).
  - *Technical Tab*: Segments, HTTP range responses, BitTorrent peer metrics.
  - *Logs Tab*: Diagnostic execution events and timestamps.

---

### 2.4 LIBRARY (`/library`)
- **Anime Catalog**: Grid or List view of indexed series with cover art and total episode counts.
- **Anime Detail View (`/library/{animeId}`)**:
  - *Header*: Title, canonical aliases, season count, total disk space consumed.
  - *Season Coverage Map*: Visual episode matrix:
    - `✓ Available` (green): Downloaded and ready on disk.
    - `— Missing` (grey): Known episode not yet downloaded (tap to search).
    - `! Review` (amber): Ambiguous title match requiring user confirmation.
    - `↓ Downloading` (blue): Currently in queue.
    - `↻ Upgrade` (purple): Higher quality release available.
  - *Quick Actions*: `Find All Missing`, `Check Upgrades`, `Storage Settings`.
- **Storage Analytics**: Pie chart of space distribution by anime, resolution, and storage target (Internal vs SD Card).

---

### 2.5 MORE & CONTROL PLANE (`/more`)
- **Collections & Playlists**: Create and manage custom anime collections.
- **Download Profiles**: Configure default preferences (Preferred Resolution, Codec, Audio channels).
- **Rule Builder**: Visual condition builder (`A AND B`, `A OR B`, `NOT`) for automated actions.
- **Automation Status**: View background automation triggers, loop protector cooldowns, and history.
- **Provider Settings**: Provider health dashboard (Nyaa latency, status, capabilities, circuit breaker state).
- **Storage Routing & Naming**: Configure destination folders and file naming templates (`Plex`, `Jellyfin`, `Kodi`).
- **Application Settings**: Language selector (English / Arabic), Dark/System theme, Network policies (Wi-Fi only).
- **Developer Diagnostics**: Export non-sensitive diagnostic bundles and run safe repair tools.

---

## 3. Screen Adaptability & Dual-Pane Layouts (Section 71 & 72)

| Screen Form Factor | Navigation Model | Search Layout | Download Details | Library View |
| :--- | :--- | :--- | :--- | :--- |
| **Phone (Portrait)** | Bottom Navigation Bar | Single-column list with filter sheet | Full-screen modal sheet | Single-column anime list |
| **Phone (Landscape)**| Compact Navigation Rail | Two-column grid | Side-sheet overlay | Two-column grid |
| **Tablet / Foldable** | Persistent Navigation Rail | Master-Detail (Results on left, Release details on right) | Dual pane with live connection graphs | Split-view: Anime catalog on left, Season coverage on right |
