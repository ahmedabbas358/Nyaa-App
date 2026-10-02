# AniFlow Core UI Components

## 1. ReleaseCard
- **Density Variants**: `Comfortable` (expanded technical badges, seeders, leechers, timestamps) and `Compact` (dense two-line overview).
- **Modes**: Standard tap navigation and Multi-Select checkbox mode.
- **Smart Highlighting**: Releases meeting preferred criteria are highlighted with the "SMART PREFERRED" amber badge.

## 2. EpisodeRow
- Displays episode number (`Episode 03`), title, and state badge (`Downloaded`, `Available`, `Downloading`).
- Includes a direct "Why?" link triggering the smart selection rationale sheet explaining why a specific release was picked.

## 3. DownloadRow
- Information-dense row modeled after desktop download managers (FDM/1DM).
- Displays linear progress bar, formatted downloaded/total size, smoothed transfer speed (`4.2 MB/s`), and remaining ETA (`00:03:18`).
- One-tap Pause / Resume / Retry control.

## 4. StatCard & Storage Bars
- Visual progress meters for local storage capacity, used vs free gigabytes, and health status indicators.
