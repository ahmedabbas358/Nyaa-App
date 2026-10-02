# AniFlow Navigation Architecture

## 1. Route Hierarchy
All application destinations are typed via `ScreenRoute`:

```text
Home (Start Destination)
 ├── Search
 │    ├── Results
 │    │    ├── Release Details (release/{id})
 │    │    ├── Anime Season (anime/{id}/season/{season})
 │    │    └── Download Plan Review (download_plan)
 │    └── Filters Bottom Sheet
 │
 ├── Downloads
 │    └── Task Details (download_details/{id})
 │
 ├── Library
 │    └── Anime Details
 │
 └── More / Settings
      ├── Collections
      ├── Favorites
      ├── Storage Management
      ├── Developer Mode
      └── Import Media Link
```

## 2. Unidirectional Data Flow (UDF)
- User interactions emit `UiEvent`.
- `ViewModel` delegates to Domain UseCases.
- Immutable `StateFlow<UiState>` feeds Jetpack Compose Composables.
- No direct database or network access is permitted from UI components.
