# AniFlow Responsive Layout Strategy

## 1. Window Size Classes
AniFlow categorizes display viewports into three responsive buckets:
- **Compact (< 600dp)**: Standard phones. Uses Bottom Navigation Bar, single-column lists, and vertical dialog stacks.
- **Medium (600dp - 840dp)**: Foldables and small tablets. Transitions to Navigation Rail on the left, dual-pane search views where appropriate.
- **Expanded (> 840dp)**: Large tablets and Chromebooks/Desktop. Displays Navigation Rail with persistent master-detail panes (e.g., search list on the left, release technical inspection pane on the right).

## 2. AppShell Adaptability
The central `AppShell` composable monitors window width and automatically swaps between:
- `NavigationBar` at the bottom for phone ergonomics.
- `NavigationRail` pinned to the left edge with a persistent Quick Import FAB for tablet ergonomics.
