# ADR-007: Automation Safety & Loop Protection Model

## Context
AniFlow provides an automated workflow engine capable of reacting to events (e.g. `NewReleaseDetected`, `DownloadCompleted`) and triggering actions such as downloading higher-quality upgrades (e.g., replacing 720p with 1080p). Without safety guardrails, recursive event cascades can cause infinite loops:
`DownloadCompleted -> Automation -> Upgrade -> Download -> DownloadCompleted -> Automation...`

## Decision
1. Implement `AutomationLoopProtector` with a mandatory `ExecutionId` passed through all cascade events.
2. Enforce a hard ceiling on recursion depth (`MAX_RECURSION_DEPTH = 3`). Any event beyond depth 3 is blocked and logged.
3. Enforce item-level concurrency locks and cooldown periods (default 60 seconds) per target anime/episode.
4. Auto-delete of replaced files is **disabled by default**, requiring explicit user confirmation.

## Alternatives Considered
- Disallowing automation upgrades entirely: Rejected as user automation is a core selling point of AniFlow.
- Relying solely on UI debouncing: Rejected because background events bypass the UI.

## Consequences
- Guaranteed zero infinite download loops.
- Deterministic and auditable automation execution.
- Safe automated upgrades without risk of runaway disk or bandwidth consumption.
