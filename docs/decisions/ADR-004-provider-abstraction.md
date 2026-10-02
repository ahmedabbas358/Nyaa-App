# ADR-004: Release Provider Abstraction (Zero Nyaa Leakage)

## Status
Accepted

## Context
While Phase 1 targets Nyaa as the initial source, AniFlow is envisioned as an extensible release management platform supporting arbitrary future sources (e.g., secondary anime trackers, custom RSS feeds, direct indexing APIs).

If Nyaa-specific URL schemes, HTML DOM structures, categories, or scrape tokens permeate the codebase, adding a second provider requires a costly architectural rewrite.

## Decision
1. Create `:provider:core` defining generic contracts:
   - `ReleaseProvider`: Universal contract for searching, fetching details, and querying capabilities.
   - `ProviderRegistry`: Registry for dynamic registration, activation, and health monitoring of providers.
   - `ProviderCapabilities`: Feature flags (`supportsSearch`, `supportsPagination`, `supportsTorrent`, etc.).
2. Restrict Nyaa-specific logic entirely to `:provider:nyaa`:
   - `NyaaClient`: HTTP transport with rate-limiting.
   - `NyaaHtmlParser`: Jsoup scraping logic.
   - `NyaaReleaseMapper`: Maps raw Nyaa DTOs to universal `Release` domain entities.
3. Prohibit any checks like `if (url.contains("nyaa.si"))` in `:domain`, `:data`, or `:feature:*`.

## Consequences
- Features interact exclusively with `ProviderRegistry` and `ReleaseProvider`.
- Adding a new provider requires adding a new `:provider:<name>` module without modifying a single line of feature or domain code.
