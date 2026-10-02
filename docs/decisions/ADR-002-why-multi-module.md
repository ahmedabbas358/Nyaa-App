# ADR-002: Why Multi-Module Architecture

## Status
Accepted

## Context
A monolithic single-module `:app` project inevitably leads to architectural degradation:
1. Feature code inadvertently accesses database DAOs or network clients directly.
2. Build times scale poorly with codebase size as incremental builds cannot leverage Gradle parallel execution and build caching.
3. Provider-specific code (e.g., Nyaa HTML parsing logic) leaks into UI components.
4. Business logic cannot be tested in pure JVM environments without Android runtime mocks.

## Decision
Modularize AniFlow from inception across 25+ targeted modules with strict directional dependencies:
- `:core:*` (common, ui, network, database, storage, logging)
- `:domain` (pure Kotlin JVM business model)
- `:data` (persistence & repository implementations)
- `:provider:*` (generic SPI + provider adapters)
- `:download:*` (generic SPI + HTTP & Torrent engines + Android service)
- `:platform:*` (Android-specific subsystems)
- `:feature:*` (isolated presentation modules)
- `:app` (composition root)

## Consequences
- **Strict Boundary Enforcement**: The compiler prevents forbidden dependency calls (e.g. `:feature:search` cannot access `:core:database`).
- **Parallel Compilation & Build Caching**: Gradle compiles independent modules concurrently.
- **Pluggability**: Adding a new provider or download engine requires zero changes to feature presentation layers.
