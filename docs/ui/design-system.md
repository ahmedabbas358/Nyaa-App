# AniFlow UI Design System

## 1. Design Philosophy
AniFlow balances YouTube-like media discovery, FDM-style dense download management, and Jellyfin-style local media organization. The UI strictly hides internal subsystems (DAOs, Tokenizers, StateMachines) and exposes intuitive, clean interfaces.

## 2. Color Palette
- **Primary Indigo (`#6366F1`)**: Main interactive accent and active tab indicators.
- **Secondary Teal (`#14B8A6`)**: Supportive highlights and badges.
- **Dark Surface (`#1E293B`)**: Cards, bottom sheets, and elevated elements.
- **Dark Background (`#0F172A`)**: Base slate canvas for optimal contrast.

### Semantic Status Colors
- **Success (`#10B981`)**: Downloaded files, complete seasons, verified health.
- **Warning (`#F59E0B`)**: Queued tasks, low storage alerts, fallback releases.
- **Error (`#F43F5E`)**: Failed downloads, critical storage (<5%), network drops.
- **Info (`#38BDF8`)**: Active downloading, live stream bitrate.

## 3. Typography Hierarchy
- **Display Large (28sp)**: App title and hero headers.
- **Headline (20sp)**: Screen titles and major section headers.
- **Title (16sp)**: Card and row titles.
- **Body (14sp)**: Main description and metadata text.
- **Caption (11sp)**: Sub-labels and muted timestamps.
- **Numeric Monospace**: Dedicated monospaced typography for download speeds (e.g. `8.4 MB/s`), ETAs (`00:54`), and byte sizes.

## 4. Spacing Grid
Built on a strict 4dp base scale:
- `xxs`: 4dp
- `xs`: 8dp
- `sm`: 12dp
- `md`: 16dp
- `lg`: 20dp
- `xl`: 24dp
- `xxl`: 32dp
- `huge`: 48dp

## 5. Shape Scale
- `Small`: 6dp (Badges, chips)
- `Medium`: 10dp (Cards, dialogs)
- `Large`: 16dp (Hero banners, sheets)
- `Pill`: 999dp (Search bar, primary action buttons)
