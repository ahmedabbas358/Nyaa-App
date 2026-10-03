# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-10-03

### Added
- **Core Architecture**: Full Clean Architecture multi-module foundation across `:domain`, `:core:*`, `:provider:*`, `:download:*`, and `:feature:*`.
- **Nyaa Discovery Engine**: Real-time anime discovery, AST query compilation, category filtering, and sorting.
- **Release Intelligence**: Smart release title parsing, confidence scoring, season/episode mapping, and release grouping.
- **Smart Selection**: User download profiles, audio/subtitle preferences, resolution/codec ranking, and automatic tie-breaking.
- **Download Planner & Orchestrator**: Atomic storage space reservation, duplicate prevention, and download task scheduling.
- **Dual Runtime Engines**: HTTP chunked multi-connection engine and BitTorrent runtime abstraction.
- **Storage & Library Engine**: Automatic folder routing, atomic file moving, media verification, and metadata indexing.
- **Control Plane & Automation**: Declarative boolean rule engine, automation loop protection, and cooldown management.
- **Full Localization**: English and Arabic (RTL) support with bidirectional Unicode formatting isolation.
- **Production Hardening**: Path traversal prevention, log redactor for secrets, circuit breaker for network reliability, and crash recovery.
- **Release Engineering**: Automated GitHub Actions CI/CD workflows, release signing configuration, and artifact validation scripts.
- **Zero Fake Data & Truthful Integration**: Completely eliminated synthetic anime entries, mock uploaders, static storage/progress figures, and dead-end buttons. Built pure empty state recovery (`AniEmptyState`) throughout the UI.

