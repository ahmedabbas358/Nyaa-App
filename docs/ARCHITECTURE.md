# AniFlow Android Architecture

## 1. Executive Summary

**AniFlow Android** is an architectural framework built for release discovery, intelligent release parsing, automatic episode grouping, explainable selection, and resilient multi-file download orchestration on Android.

The system is designed from day zero to be:
1. **Multi-Module**: Strictly isolated boundaries between Presentation, Domain, Data, Providers, and Download engines.
2. **Provider-Agnostic**: Nyaa is the initial release provider adapter, but zero Nyaa-specific types or logic leak into Presentation or Domain.
3. **Resilient Background Execution**: Decoupled from Activity lifecycles using Android Foreground Services and Room persistence.
4. **Transparent & Controllable**: Auto-selection is separated from auto-queuing. All selection criteria are scored and explained with clear positive and negative rationales.

---

## 2. Layered Organization

```
┌────────────────────────────────────────────────────────┐
│               01_PRESENTATION LAYER                    │
│   Jetpack Compose · Material 3 · ViewModels · Flow     │
│   Screens: Home, Search, Downloads, Library, Settings  │
└──────────────────────────┬─────────────────────────────┘
                           │ uses Use Cases & StateFlow
┌──────────────────────────▼─────────────────────────────┐
│              02_BUSINESS MODEL (DOMAIN)                │
│   Entities · Value Objects · Enums · Use Cases         │
│   GroupingEngine · SelectionEngine · ReleaseParser     │
│   Repository Interfaces · DownloadStateMachine         │
│   (Zero Android SDK or Network dependencies)           │
└──────────────────────────▲─────────────────────────────┘
                           │ implements Interfaces
┌──────────────────────────┴─────────────────────────────┐
│                 03_BACKEND (DATA & RUNTIME)            │
│   Room Database · DataStore · Storage Manager          │
│   Nyaa Provider · OkHttp Client · HTTP Download Engine │
│   Foreground Service · Notifications · Rate Limiter    │
└────────────────────────────────────────────────────────┘
```

---

## 3. Module Hierarchy

| Module | Type | Responsibilities |
|---|---|---|
| `:app` | Android App | Application entry point, Hilt DI graph, Top-level Navigation Host |
| `:core:common` | Kotlin JVM | `AniFlowResult`, `ErrorType`, `FilenameSanitizer`, Dispatchers, Logger |
| `:core:ui` | Android Library | Design system tokens (`Color`, `Theme`), Reusable UI components |
| `:core:database` | Android Library | Room database, DAOs, entities, indexes, converters |
| `:core:network` | Kotlin JVM | OkHttp client factory, rate limiter, retry policy |
| `:core:storage` | Android Library | `StorageManager`, disk space pre-flight validation |
| `:core:cache` | Kotlin JVM | Cache management policies and TTL invalidation |
| `:domain` | Kotlin JVM | Core business model: entities, use cases, parser, grouping, selection |
| `:data` | Android Library | Repository implementations, entity mappers, duplicate detection |
| `:provider:core` | Kotlin JVM | `ReleaseProvider`, `ProviderRegistry`, capabilities |
| `:provider:nyaa` | Kotlin JVM | Nyaa HTML & RSS scrapers, raw release mapper |
| `:download:core` | Kotlin JVM | `DownloadOrchestrator`, concurrency slots, queue |
| `:download:http` | Android Library | Segmented resume-capable HTTP engine (`.part` files) |
| `:download:torrent`| Android Library | Torrent engine interface abstraction |
| `:platform` | Android Library | `DownloadForegroundService`, notifications |
| `:feature:home` | Android Library | Smart Home dashboard and metrics |
| `:feature:search` | Android Library | Multi-criteria search, filters, batch planning |
| `:feature:release` | Android Library | Release details and swarm inspection |
| `:feature:downloads`| Android Library | Active queue dashboard, pause/resume/cancel |
| `:feature:library` | Android Library | Media index, directory scanner |
| `:feature:settings` | Android Library | Concurrency, storage directory, network rules |
| `:testing` | Kotlin JVM | Test suite for parser, grouping, and state machines |
