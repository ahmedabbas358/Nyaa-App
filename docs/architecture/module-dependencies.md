# Module Dependencies & Graph

## 1. Top-Level Module Map

AniFlow Android is decomposed into 25+ modules across three primary layers:
- **Presentation Layer**: `:feature:*`, `:core:ui`, `:app`
- **Business Model Layer**: `:domain`
- **Backend Layer (Data & Runtime)**: `:data`, `:core:*`, `:provider:*`, `:download:*`, `:platform:*`

---

## 2. ASCII Dependency Diagram

```text
                                 :app (Composition Root)
                                   │
      ┌────────────────────────────┼───────────────────────────┐
      │                            │                           │
      ▼                            ▼                           ▼
  :feature:*                   :platform:*                 :core:ui
(home, search, release,       (notifications, share,       (theme, components,
 downloads, library, etc.)     network, storage)            badges, cards)
      │                            │                           │
      └──────────────┬─────────────┴─────────────┬─────────────┘
                     ▼                           ▼
                  :domain                     :core:common
             (Pure Business Logic)         (Universal Utilities)
                     ▲                           ▲
                     │ implements                │
                     │                           │
                  :data ─────────────────────────┘
              (Repositories)
                     │
      ┌──────────────┼──────────────┐
      ▼              ▼              ▼
:core:network  :core:database  :core:storage
      ▲
      │
:provider:core ◄────────────── :provider:nyaa
(Generic Provider SPI)        (Nyaa Adapter)

:download:core (Generic Download SPI)
      ▲              ▲              ▲
      │              │              │
:download:http :download:torrent :download:service
```

---

## 3. Directional Flow of Invocations

```text
UI Screen (e.g. SearchScreen)
    ↓ sends event
ViewModel (e.g. SearchViewModel)
    ↓ executes
Use Case (e.g. SearchReleasesUseCase)
    ↓ calls contract
Repository Interface (e.g. ReleaseRepository)
    ↓ implemented by
Repository Implementation (:data)
    ↓ delegates to
ProviderRegistry & ReleaseProvider (:provider:core / :provider:nyaa)
    ↓ fetches via
OkHttp & Jsoup (:core:network / :provider:nyaa)
    ↓ maps DTO to Domain Entity
Domain Release (:domain)
    ↓ passes to
GroupingEngine & SelectionEngine (:domain)
    ↓ returns to
ViewModel → StateFlow → Compose UI
```
