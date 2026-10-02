# AniFlow — Final Implementation Order & Engineering Roadmap

> **Enforcing STEP 15 Section 121 & 122.**  
> *The Strict Rule: Contract → Domain → Test → Implementation → Integration → UI.*

---

## 1. Implementation Phasing Strategy

Building dozens of disconnected screens without working underlying domain models leads to fragile architectures. Development strictly follows this 13-phase dependency order:

```text
Phase 1: Foundation Layer
     ↓
Phase 2: Database & Persistence Layer
     ↓
Phase 3: Nyaa Provider Adapter Layer
     ↓
Phase 4: Release Intelligence & Grouping Layer
     ↓
Phase 5: Search Discovery & UI Layer
     ↓
Phase 6: Selection & "Why" Explanation Layer
     ↓
Phase 7: Download Orchestration & Dual Runtimes
     ↓
Phase 8: Storage, SAF & Media Library Layer
     ↓
Phase 9: Profiles, Rules & Control Plane
     ↓
Phase 10: Automation & Continuous Monitoring
     ↓
Phase 11: Full End-to-End System Wiring
     ↓
Phase 12: Production Hardening, Chaos & QA
     ↓
Phase 13: Release Engineering, Signing & CI/CD
```

---

## 2. Phase-by-Phase Execution Breakdown

### Phase 1: Foundation Layer
- Set up Gradle build conventions (`build-logic`) and Version Catalog (`libs.versions.toml`).
- Solidify `:core:common`: Typed IDs (`AnimeId`, `ReleaseId`, `EpisodeId`), Result wrappers (`AniFlowResult`), and `DispatcherProvider`.
- Setup `:core:logging` with `LogRedactor` and `SecureLogger`.

### Phase 2: Database & Persistence Layer
- Define Room Database (`AniFlowDatabase`) with all core entities (`ReleaseEntity`, `DownloadTaskEntity`, `AnimeEntity`, `EpisodeEntity`).
- Implement DAOs with indexed foreign keys, type converters, and in-memory test fixtures.

### Phase 3: Nyaa Provider Adapter Layer
- Implement `NyaaHttpClient` with connection timeouts and `RequestLimiter`.
- Implement `NyaaHtmlSearchParser` and `NyaaHtmlDetailsParser` using Jsoup.
- Integrate `ProviderCircuitBreaker` and capability contracts in `:provider:core`.

### Phase 4: Release Intelligence & Grouping Layer
- Build `ReleaseParserImpl` (Tokenizer → Normalizer → Extractors for resolution, codec, audio, batch).
- Implement confidence scoring (`High`, `Medium`, `Low`, `Review Required`).
- Implement `ReleaseGroupingService` for mapping releases to seasons and detecting missing episodes.

### Phase 5: Search Discovery & UI Layer
- Build `SearchScreen` and `SearchViewModel` using Jetpack Compose and Material 3.
- Connect Search UI to `SearchReleasesCoordinatorUseCase` with quick filter chips and grouped/flat display modes.
- Implement Release Cards with strict 8-level information hierarchy.

### Phase 6: Selection & "Why" Explanation Layer
- Implement `PreferenceResolver` evaluating user download profiles.
- Implement hard eligibility constraint disqualifiers.
- Generate structured human-readable explanations (`SelectionResult.explanation`).

### Phase 7: Download Orchestration & Dual Runtimes
- Solidify `DownloadStateMachine` with strict transition enforcement.
- Implement `StorageReservationManager` for atomic space allocation.
- Implement chunked multi-connection `HttpDownloadEngine` and `TorrentEngine` abstraction.
- Integrate `ProgressPersistenceThrottler` and Android Foreground Service.

### Phase 8: Storage, SAF & Media Library Layer
- Implement Scoped Storage integration via Storage Access Framework (`StorageTarget`).
- Enforce path traversal protection via `PathSanitizer`.
- Implement `MediaOrganizationService` with naming templates (`Plex`, `Jellyfin`, `Kodi`).
- Build Library catalog, season coverage map, and `PartialFileIntegrityManager`.

### Phase 9: Profiles, Rules & Control Plane
- Build declarative boolean rule AST evaluator (`RuleTreeEvaluator`).
- Implement visual rule builder UI and profile management screens.
- Build side-by-side release comparison surface.

### Phase 10: Automation & Continuous Monitoring
- Implement `AutomationLoopProtector` with recursion depth limit (`MAX_DEPTH = 3`) and cooldowns.
- Implement background scheduled search workers for airing anime.
- Implement automated quality upgrade workflows with safe non-destructive quarantine.

### Phase 11: Full End-to-End System Wiring
- Connect all layers end-to-end:
  `Search → Nyaa → Normalize → Group → Plan → Queue → Transfer → Verify → Move → Library`.
- Verify seamless reactive UI state propagation.

### Phase 12: Production Hardening, Chaos & QA
- Execute `ParserRegressionSuite` and `ParserFuzzTests`.
- Execute `ReliabilityAndChaosTests` (Storage reservation, Circuit breaker, State machine).
- Execute `SecurityAndSanitizationTests` (Path traversal, reserved device names, log redaction).
- Verify Arabic RTL bidirectional text rendering with `BidiFormatter`.

### Phase 13: Release Engineering, Signing & CI/CD
- Configure GitHub Actions CI/CD workflows (`ci.yml`, `release.yml`, `nightly.yml`).
- Configure environment-driven release signing in `app/build.gradle.kts`.
- Validate R8 minification, ProGuard rules, and non-debuggable APK/AAB outputs.
- Execute the final 14-step acceptance scenario.
