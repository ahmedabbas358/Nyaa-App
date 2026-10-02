# Privacy Policy for AniFlow

AniFlow is committed to protecting user privacy. This policy outlines what data the application collects, how it is handled, and what data leaves your device.

---

## 1. Zero External Telemetry by Default

- AniFlow **does not** collect, store, or transmit personal information, analytics, device identifiers, or user behavior to external telemetry servers.
- The application functions entirely offline for local library management, downloaded media playback, and rule configurations.

---

## 2. External Provider Interactions

- When you perform an anime search, your search query is sent directly to the configured provider (e.g. Nyaa) via HTTPS to fetch release listings.
- No personal user identifiers, email addresses, or device identifiers are sent with search requests.
- All network requests use standard HTTPS encryption.

---

## 3. Local Data Storage

All application state is stored locally on your device in private SQLite / Room databases:
- **Download History & Queue**: Stored strictly on device.
- **Library Index & File Metadata**: Stored strictly on device.
- **Custom Profiles & Automation Rules**: Stored strictly on device.
- **Cached Search Results**: Retained in local database cache with automated expiration.

---

## 4. Permissions

AniFlow requests only the minimum permissions required for core operations:
- `INTERNET`: Required to communicate with release providers and download media.
- `ACCESS_NETWORK_STATE`: Required to enforce network policies (e.g. download on unmetered Wi-Fi only).
- `POST_NOTIFICATIONS`: Required on Android 13+ to notify the user of download completions and critical errors.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`: Required to run uninterrupted background downloads.
- Storage Access: Uses Android Storage Access Framework (SAF) scoped directories selected by the user.

---

## 5. Data Deletion

You retain full control over your data:
- Clearing application data or uninstalling the app permanently purges all databases, cached search results, and local settings.
- Media files in your chosen external library directory remain under your ownership and are never deleted unless you explicitly invoke file deletion in the app.
