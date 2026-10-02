# ADR-005: Download Engine Abstraction

## Status
Accepted

## Context
AniFlow must support heterogeneous transport protocols:
1. Direct HTTP/HTTPS downloads (single connection and multi-segment range requests).
2. BitTorrent protocol (magnet URIs, torrent files, swarm peer management).

Coupling the UI or repository directly to OkHttp streams or a native BitTorrent library (`libtorrent`) leads to fragile state handling, difficult recovery after process death, and complex testing.

## Decision
1. Create `:download:core` defining generic abstractions:
   - `DownloadEngine`: Universal interface (`start`, `pause`, `resume`, `cancel`, `observeProgress`).
   - `DownloadEngineRegistry`: Routes tasks to the appropriate engine based on release metadata (`isTorrent` vs `HTTP`).
   - `DownloadOrchestrator`: Controls global concurrency slots, priority queues, and state machine validation.
2. Isolate protocol-specific implementations:
   - `:download:http`: Range requests, byte streaming, resume offsets, writing to `.part` files.
   - `:download:torrent`: Dedicated BitTorrent engine wrapper.
3. Decouple Android process lifecycle into `:download:service`:
   - Runs `DownloadForegroundService` and updates system notifications without containing download business logic.

## Consequences
- The Presentation layer (`DownloadsViewModel`, `DownloadsScreen`) interacts strictly with `DownloadRepository` and `DownloadOrchestrator`.
- The application can switch or upgrade underlying engines (e.g. replacing an HTTP engine with an alternate client, or swapping torrent libraries) without touching the UI or database schemas.
