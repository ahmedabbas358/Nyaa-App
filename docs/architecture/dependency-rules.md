# Dependency Rules & Boundary Enforcement

## 1. Core Architectural Axioms

1. **High-Level Policy Does Not Depend on Low-Level Detail**:
   - `:domain` never depends on `:data`, `:core:database`, `:core:network`, or Android SDK.
   - All external concerns implement interfaces declared in `:domain` or `:provider:core` / `:download:core`.

2. **Feature Isolation**:
   - Feature modules never depend on other feature modules.
   - Feature modules never depend directly on `:data`, `:core:database`, `:provider:nyaa`, or download engines.
   - Inter-feature navigation and wiring occur exclusively in `:app`.

3. **Data Layer Boundaries**:
   - `:data` never depends on `:feature:*`.
   - `:data` implements `:domain` repository interfaces.

---

## 2. Allowed vs Forbidden Dependencies Matrix

| Module | Allowed Direct Dependencies | Strictly Forbidden Dependencies |
|---|---|---|
| **`:domain`** | `:core:common` | Android SDK, Compose, Room, OkHttp, Retrofit, Hilt, Nyaa, libtorrent, `:data`, `:feature:*` |
| **`:core:common`** | Standard Kotlin runtime, Coroutines Core, Serialization | Android SDK, UI, Database, Network, Features, Domain |
| **`:core:ui`** | `:core:common`, `:domain`, Compose BOM, Material3, Coil | Repositories, DAOs, OkHttp, Network, Scrapers, Features |
| **`:core:network`** | `:core:common`, OkHttp, Coroutines | UI, Compose, Room, Scrapers, Features, Domain |
| **`:core:database`** | `:core:common`, `:domain`, Room Runtime/KTX | Compose, UI, OkHttp, Features, Scrapers, Download Engines |
| **`:core:storage`** | `:core:common`, `:domain`, Android Context (SAF) | UI, Compose, Room, OkHttp, Scrapers, Features |
| **`:core:logging`** | Standard Kotlin runtime | UI, Android, Database, Network, Features |
| **`:provider:core`** | `:domain`, `:core:common` | Nyaa-specific classes, OkHttp, UI, Room |
| **`:provider:nyaa`** | `:provider:core`, `:core:network`, `:core:common`, `:domain`, Jsoup | Room, Compose, UI, `:feature:*`, `:download:*` |
| **`:download:core`** | `:domain`, `:core:common` | Android SDK, OkHttp, libtorrent, UI, Room |
| **`:download:http`** | `:download:core`, `:core:network`, `:core:storage`, `:core:common` | libtorrent, UI, Compose, Room |
| **`:download:torrent`**| `:download:core`, `:core:storage`, `:core:common` | OkHttp, UI, Compose, Room |
| **`:download:service`**| `:download:core`, `:core:common`, `:platform:*` | Business logic, Rule evaluation, Grouping, Nyaa |
| **`:feature:*`** | `:domain`, `:core:ui`, `:core:common` | `:data`, `:core:database`, `:core:network`, `:provider:nyaa`, `:download:http` |
| **`:app`** | All modules (Composition Root) | Circular dependencies |

---

## 3. Boundary Crossing Violations

The following types MUST NEVER leak across architectural boundaries:

| Leaked Type | Why Forbidden | Target Abstraction |
|---|---|---|
| `Room Entity` (e.g. `ReleaseEntity`) | Couples UI/Domain to database schema | Map to `Domain Entity` or `UiModel` |
| `Nyaa DTO` (e.g. `NyaaRawRelease`) | Leaks source scraper structure | Map to `Release` via `NyaaReleaseMapper` |
| `OkHttp Response / Request` | Leaks networking framework | Encapsulated inside `Client` / `Engine` |
| `libtorrent / Native Objects` | Leaks torrent client specifics | Encapsulated behind `TorrentEngine` |
| `java.io.File / Direct Paths` | Breaks Storage Access Framework and testability | Abstracted through `StorageManager` |
