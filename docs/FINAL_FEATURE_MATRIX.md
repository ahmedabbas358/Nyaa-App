# AniFlow — Final Feature Matrix & Scope Freeze

> **Enforcing STEP 15 Section 3, 4, 100–105 & 120.**  
> *P0 and P1 scopes are officially FROZEN for v1.0.0.*

---

## 1. Feature Priority Legend

- **P0 (Core Product)**: Non-negotiable core functionality required for an operational release.
- **P1 (Essential Advanced)**: Advanced capabilities providing the platform's primary competitive value.
- **P2 (Important Enhancement)**: High-value polish and extended workflows scheduled for post-v1.0.0.
- **P3 (Optional)**: Convenience features and cosmetic enhancements.
- **Future**: Capabilities outside current mobile scope (e.g. Desktop, Cloud sync, Social).

---

## 2. Global Feature Matrix

| Domain | Feature | Priority | Status | Module Owner | Dependencies | Target Release | Test Suite |
| :--- | :--- | :---: | :---: | :--- | :--- | :---: | :--- |
| **Discovery** | Nyaa Search & Pagination | P0 | Implemented | `:provider:nyaa` | `:core:network` | v1.0.0 | Unit, Integration |
| **Discovery** | Search Presets & Templates | P1 | Implemented | `:feature:search` | `:domain` | v1.0.0 | Unit, UI |
| **Discovery** | Uploader Search Mode | P1 | Implemented | `:provider:nyaa` | `:domain` | v1.0.0 | Integration |
| **Discovery** | Visual AST Query Builder | P1 | Implemented | `:feature:search` | `:domain` | v1.0.0 | Unit, UI |
| **Discovery** | Universal Global Search | P2 | Planned | `:feature:search` | `:core:database` | v1.1.0 | Unit, Integration |
| **Intelligence** | Title Parsing Pipeline | P0 | Implemented | `:domain` | None | v1.0.0 | ParserRegressionSuite, Fuzz |
| **Intelligence** | Confidence Scoring (High/Low/Review) | P0 | Implemented | `:domain` | None | v1.0.0 | Unit |
| **Intelligence** | Episode & Range Detection | P0 | Implemented | `:domain` | None | v1.0.0 | Unit, Regression |
| **Intelligence** | Smart Anime & Season Grouping | P0 | Implemented | `:domain` | None | v1.0.0 | Unit, Integration |
| **Intelligence** | Batch vs Single Episode Resolution | P1 | Implemented | `:domain` | None | v1.0.0 | Unit |
| **Intelligence** | Release Comparison (Side-by-Side) | P1 | Implemented | `:feature:release` | `:domain` | v1.0.0 | UI |
| **Intelligence** | Uploader Observed History Profile | P1 | Implemented | `:domain` | `:core:database` | v1.0.0 | Unit |
| **Selection** | User Download Profiles | P0 | Implemented | `:domain` | `:core:database` | v1.0.0 | Unit, Integration |
| **Selection** | Hard Constraints Disqualification | P0 | Implemented | `:domain` | None | v1.0.0 | SelectionAndRuleTests |
| **Selection** | Explainable "Why" Selection Layer | P0 | Implemented | `:domain` | None | v1.0.0 | Unit |
| **Selection** | ConsistencyFirst Mode (Same Group) | P1 | Implemented | `:domain` | None | v1.0.0 | Unit |
| **Selection** | Season Optimization Calculator | P1 | Implemented | `:domain` | None | v1.0.0 | Unit |
| **Download** | Priority Queue & Scheduling | P0 | Implemented | `:download:core` | `:core:database` | v1.0.0 | EngineTests |
| **Download** | Atomic Storage Space Reservation | P0 | Implemented | `:download:core` | None | v1.0.0 | ReliabilityAndChaosTests |
| **Download** | Strict Download State Machine | P0 | Implemented | `:download:core` | None | v1.0.0 | ReliabilityAndChaosTests |
| **Download** | Chunked Range HTTP Engine | P0 | Implemented | `:download:http` | `:core:network` | v1.0.0 | Unit, Integration |
| **Download** | BitTorrent Engine Abstraction | P0 | Implemented | `:download:torrent` | None | v1.0.0 | Unit |
| **Download** | Selective Torrent File Picking | P1 | Implemented | `:download:torrent` | `:download:core` | v1.0.0 | Integration |
| **Download** | Progress Persistence Throttling | P0 | Implemented | `:download:core` | None | v1.0.0 | ReliabilityAndChaosTests |
| **Download** | Download Crash Reconciliation | P0 | Implemented | `:download:core` | `:core:database` | v1.0.0 | FinalAcceptanceScenarioTest |
| **Storage** | SAF Scoped Storage Integration | P0 | Implemented | `:core:storage` | None | v1.0.0 | StorageAndLibraryTests |
| **Storage** | Path Traversal Protection (`PathSanitizer`) | P0 | Implemented | `:core:common` | None | v1.0.0 | SecurityAndSanitizationTests |
| **Storage** | Media Naming Templates (Plex/Jellyfin) | P1 | Implemented | `:domain` | None | v1.0.0 | Unit |
| **Storage** | Safe File Organization & Atomic Move | P0 | Implemented | `:download:core` | `:core:storage` | v1.0.0 | EndToEndIntegrationTests |
| **Library** | Local Media Library Indexing | P0 | Implemented | `:feature:library` | `:core:database` | v1.0.0 | StorageAndLibraryTests |
| **Library** | Season Coverage Map (Missing/Available)| P0 | Implemented | `:feature:library` | `:domain` | v1.0.0 | UI, Unit |
| **Library** | Missing Episode Search Flow | P1 | Implemented | `:feature:library` | `:domain` | v1.0.0 | Integration |
| **Library** | Watched State vs Download State | P2 | Planned | `:domain` | `:core:database` | v1.1.0 | Unit |
| **Library** | Built-in ExoPlayer Video Player | P2 | Planned | `:feature:player` | `:feature:library` | v1.1.0 | UI |
| **Control Plane**| Declarative Rule Tree Evaluator (AST) | P0 | Implemented | `:domain` | None | v1.0.0 | SelectionAndRuleTests |
| **Control Plane**| Automation Loop & Depth Protector | P0 | Implemented | `:domain` | None | v1.0.0 | ReliabilityAndChaosTests |
| **Control Plane**| Automated Quality Upgrade Workflow | P1 | Implemented | `:domain` | `:download:core` | v1.0.0 | EndToEndIntegrationTests |
| **Control Plane**| Safe Config Import & Export Sanitizer | P0 | Implemented | `:domain` | `:core:common` | v1.0.0 | SecurityAndSanitizationTests |
| **Resilience** | Provider Circuit Breaker | P0 | Implemented | `:core:network` | None | v1.0.0 | ReliabilityAndChaosTests |
| **Resilience** | Sensitive Log Masking (`LogRedactor`) | P0 | Implemented | `:core:logging` | None | v1.0.0 | SecurityAndSanitizationTests |
| **Resilience** | Bounded Crash Reporting Service | P0 | Implemented | `:platform` | `:core:logging` | v1.0.0 | Unit |
| **Resilience** | Release Health Metrics Monitor | P1 | Implemented | `:platform` | None | v1.0.0 | Unit |
| **UI/UX** | YouTube-like Media & Browse Shell | P0 | Implemented | `:app` | `:core:ui` | v1.0.0 | PresentationAndUiTests |
| **UI/UX** | Full English & Arabic RTL Localization | P0 | Implemented | `:core:ui` | None | v1.0.0 | UI, Localization |
| **UI/UX** | Unicode Bidirectional Isolator (`BidiFormatter`) | P0 | Implemented | `:core:ui` | None | v1.0.0 | Unit |
| **UI/UX** | Bulk Multi-Select & Batch Operations | P1 | Implemented | `:feature:downloads` | `:download:core` | v1.0.0 | UI |
| **UI/UX** | Tablet Master-Detail & Foldable Layouts| P2 | Planned | `:core:ui` | None | v1.1.0 | UI |
| **Platform** | GitHub Actions CI/CD Pipeline | P0 | Implemented | `.github/workflows`| None | v1.0.0 | CI Suite |
| **Platform** | Build Logic Conventions (`build-logic`) | P0 | Implemented | `build-logic` | None | v1.0.0 | Build System |
| **Platform** | Production R8 & ProGuard Verification | P0 | Implemented | `:app` | None | v1.0.0 | Release Check |
| **Future** | Desktop (Windows / macOS / Linux) | Future | Deferred | None | None | v2.x | None |
| **Future** | Cross-device Cloud Account Sync | Future | Deferred | None | None | v2.x | None |
