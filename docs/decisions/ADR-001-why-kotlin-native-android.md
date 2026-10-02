# ADR-001: Why Kotlin Native Android

## Status
Accepted

## Context
AniFlow requires tight integration with Android platform capabilities:
1. Long-running, resource-efficient background download management via Android Foreground Services.
2. Low-level file streaming, random access writes (`RandomAccessFile`), and native Storage Access Framework (SAF) compliance.
3. High frame-rate reactive UI (Jetpack Compose) capable of rendering thousands of releases without UI thread stutter.
4. Future C++ JNI integration with native torrent libraries (`libtorrent`).

Cross-platform frameworks (Flutter, React Native) introduce runtime bridging overhead, lack native Foreground Service lifecycle guarantees on modern Android OS versions (Android 12-15+ battery optimization restrictions), and complicate direct JNI bindings.

## Decision
Build AniFlow as a 100% Native Android application utilizing:
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose with Material 3
- **Async & Reactive**: Coroutines + StateFlow
- **Architecture**: Clean Architecture with Unidirectional Data Flow (UDF)

## Consequences
- Direct, unhindered access to modern Android background APIs (Foreground Services, Notifications, Storage Access Framework).
- Zero bridging penalties for high-frequency progress updates (download speed, progress, torrent piece availability).
- Clean unit and instrumentation testing via standard Android/Kotlin toolchains.
