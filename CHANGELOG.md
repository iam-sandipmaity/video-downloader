# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] — 2026-04-05

### Added
- **Room database** for persistent download queue and history — tasks and completed downloads survive app kills and restarts
- **Dark theme support** — toggle light/dark appearance in Settings; preference is saved and applied on next launch
- **WorkManager exponential backoff** — transient network failures and CDN 403s retry with automatic exponential backoff (starts at 10s, max retries)
- **R8 shrinking + minification** — release builds are now minified and shrunk with comprehensive ProGuard rules for Hilt, Compose, kotlinx-serialization, yt-dlp-android, and FFmpeg
- **Media scan on download completion** — downloaded files are copied to the public `/sdcard/Download/LocalDownloader/` folder on Android 11+ (via `MediaStore`) so they appear in file managers
- **JSON caching of download options** in Room for reliable resume capability after app restarts

### Changed
- **Scoped Storage compliance** — removed deprecated `Environment.getExternalStoragePublicDirectory` for Android 10 and below; downloads use the appropriate storage path for each API level
- **Removed use-case layer** — five thin pass-through use cases (AnalyzeUrl, StartDownload, ManageSettings, ObserveDownloadQueue, ConvertMedia) were replaced with direct repository calls for simpler, flatter code
- **Non-blocking task store** — in-memory `MutableStateFlow` persists to Room asynchronously, eliminating blocking suspend calls from Worker callbacks
- **Log privacy** — removed external storage log mirrors; logs now write only to internal storage (`filesDir/logs/`)

### Fixed
- Kotlin serialization compatibility — aligned `kotlinx-serialization-json` to 1.6.3 for Kotlin 1.9.24
- CameraX import accidentally left in `MainActivity`
- Missing `File` import in `DownloadWorker`
- Worker `suspend` calls to `DownloadTaskStore` resolved by making operations non-suspending with async Room persistence
- Incorrect import paths for `WorkerKeys` and `DownloadTaskStore` internal modules

### Technical
- Added `@Serializable` annotation to `DownloadOptions` for Room JSON serialization
- Added `DownloadTaskEntity`, `DownloadTaskDao`, `AppDatabase` Room components
- Updated `DownloadTaskStore` to hybrid in-memory + Room async-backing pattern
- `DownloadWorker` now triggers `copyToPublicDownloads` on successful completion for Android 11+ visibility

---

## [1.1.0] — (pre-1.2.0 baseline)

Initial release with:
- Video and audio downloading via yt-dlp
- Format picker (quality, stream type, container)
- Download queue with progress tracking
- FFmpeg-based conversion and compression
- Compose UI with Material3
- Hilt DI, WorkManager, DataStore settings
