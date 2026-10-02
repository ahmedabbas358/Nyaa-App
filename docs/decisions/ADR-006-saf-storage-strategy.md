# ADR-006: Scoped Storage and Storage Access Framework (SAF) Strategy

## Context
Android 10+ (API 29+) and Android 11+ (API 30+) enforce scoped storage rules. Broad filesystem access (`MANAGE_EXTERNAL_STORAGE`) is restricted by Google Play policies and poses privacy and file overwriting risks. AniFlow must support storing anime library files on internal storage, SD cards, and external USB volumes.

## Decision
1. We utilize Android's **Storage Access Framework (SAF)** with persisted URI tree permissions (`takePersistableUriPermission`) for user-selected storage roots.
2. Inside the authorized directory tree, physical file operations are handled atomically via `java.io.File` / `DocumentFile` abstraction.
3. Every path resolution passes through `PathSanitizer.ensureInsideStorageRoot` to guarantee no path traversal exploits can escape the authorized tree.

## Alternatives Considered
- Requesting broad `MANAGE_EXTERNAL_STORAGE` permission: Rejected due to Play Store rejection risk and security overreach.
- Restricting downloads exclusively to app-private cache directories: Rejected because uninstalling the app would delete the user's entire multi-gigabyte media library.

## Consequences
- The user has complete control over their download directory (internal, SD card, external drive).
- Media files survive app uninstall/reinstall.
- Zero path traversal vulnerability.
