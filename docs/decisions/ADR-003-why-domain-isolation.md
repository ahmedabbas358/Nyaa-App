# ADR-003: Why Domain Isolation (Pure Kotlin JVM)

## Status
Accepted

## Context
When business logic references Android framework classes (`Context`, `Activity`, `File`, `Toast`, `SharedPreferences`), it becomes tightly coupled to Android OS versions, lifecycle quirks, and requires expensive Android emulator/Robolectric test runners.

AniFlow's core value proposition resides in pure algorithms:
1. Release title parsing and tokenization (`ReleaseParser`).
2. Multi-tier anime, season, and episode clustering (`GroupingEngine`).
3. Explainable multi-attribute scoring and candidate selection (`SelectionEngine`).
4. Discrete state transitions (`DownloadStateMachine`).

## Decision
Constrain `:domain` to be a 100% pure Kotlin JVM module:
- Prohibit imports of Android SDK (`android.*`).
- Prohibit imports of third-party transport or database libraries (`okhttp3.*`, `androidx.room.*`).
- Prohibit dependency injection framework annotations (`@Inject`, `@HiltAndroidApp`).
- Express all contracts as pure interfaces (`Repository`, `Engine`, `Service`).

## Consequences
- 100% of AniFlow's business logic, title parsing, grouping algorithms, and state machines execute in standard JVM unit tests within milliseconds.
- Platform changes or database migrations cannot destabilize core domain logic.
