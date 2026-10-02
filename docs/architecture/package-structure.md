# Internal Package Structure by Module

## 1. Domain Module (`:domain`)
```text
com.aniflow.domain/
├── entity/          # Universal Entities (Anime, Season, Episode, Release, DownloadTask, Plan)
├── valueobject/     # Value Objects (Resolution, Codec, AudioTrack, SubtitleTrack, FileSize)
├── enums/           # Domain Enums (ReleaseType, DownloadState, EpisodeStatus, Priority)
├── repository/      # Repository Contracts (ReleaseRepository, DownloadRepository, SettingsRepository)
├── service/         # Domain Services (ReleaseParser, GroupingEngine, SelectionEngine, FileOrg)
├── usecase/         # Interactors (SearchReleases, GroupReleases, SelectReleases, BuildPlan)
└── state/           # State Machines (DownloadStateMachine, ProviderStateMachine)
```

## 2. Data Module (`:data`)
```text
com.aniflow.data/
├── repository/      # Repository Implementations (ReleaseRepositoryImpl, DownloadRepositoryImpl)
├── mapper/          # Entity <-> Domain mappers (ReleaseEntityMapper, DownloadEntityMapper)
├── datasource/      # Local and Remote Data Sources
└── cache/           # Persistence cache policies
```

## 3. Provider Modules
```text
:provider:core
com.aniflow.provider.core/
├── contract/        # ReleaseProvider SPI
├── registry/        # ProviderRegistry, DefaultProviderRegistry
├── model/           # Generic provider models
└── capability/      # ProviderCapabilities

:provider:nyaa
com.aniflow.provider.nyaa/
├── client/          # NyaaClient (OkHttp, RateLimiter, RetryPolicy)
├── parser/          # NyaaHtmlParser (Jsoup), NyaaRssParser
├── mapper/          # NyaaReleaseMapper (Raw -> Domain Release)
├── model/           # NyaaRawRelease DTOs
└── NyaaProvider.kt  # Implementation of ReleaseProvider
```

## 4. Download Modules
```text
:download:core
com.aniflow.download.core/
├── contract/        # DownloadEngine, TorrentEngine interfaces
├── registry/        # DownloadEngineRegistry
├── orchestrator/    # DownloadOrchestrator (concurrency, queue processing)
└── model/           # DownloadRequest, DownloadCapabilities, DownloadProgress

:download:http
com.aniflow.download.http/
└── HttpDownloadEngine.kt # Range requests, resume, speed, .part writing

:download:torrent
com.aniflow.download.torrent/
└── TorrentEngine.kt      # libtorrent abstraction

:download:service
com.aniflow.download.service/
└── DownloadForegroundService.kt # Android Foreground Service lifecycle
```

## 5. Platform Modules
```text
:platform:notifications -> com.aniflow.platform.notifications.DownloadNotificationManager
:platform:network       -> com.aniflow.platform.network.AndroidNetworkMonitor
:platform:storage       -> com.aniflow.platform.storage.AndroidPlatformStorageManager
:platform:share         -> com.aniflow.platform.share.AndroidShareHandler
```

## 6. Feature Modules (`:feature:*`)
```text
com.aniflow.feature.<feature_name>/
├── <Feature>Screen.kt
├── <Feature>ViewModel.kt
├── <Feature>UiState.kt
├── <Feature>UiEvent.kt
├── components/      # Feature-specific Composables
└── navigation/      # Navigation destination routes
```
