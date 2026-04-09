# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4.0] — 2026-04-09

### Added
- **Download button state management** — button disables after click to prevent duplicate downloads, shows "Please wait..." with faded appearance, re-enables when user changes format/quality/type/container/audio settings, auto-re-enables after 6-second timeout
- **Cache management in Settings** — displays current cache size with clear button to free up storage
- **Enhanced Help Screen** — comprehensive documentation covering Downloads, Converter, Compressor, Navigation tabs, Settings, Troubleshooting, and About sections
- **YouTube DASH video+audio download support** — improved format selection for higher resolution downloads
- **Artifact cleanup workflow** — GitHub Actions workflow automatically deletes artifacts older than 2 days

### Changed
- **YouTube cookie authentication** — cookies are now applied when YouTube auth is enabled OR PO token is provided (previously only worked with PO token)
- **Converter output location** — converted files now copied to public Downloads/LocalDownloader folder for easy access
- **Compressor output location** — compressed files now copied to public Downloads/LocalDownloader folder for easy access
- **Help page** — completely rewritten with much more detailed information

### Fixed
- **Cookie auth bug** — cookies were not being applied for age-gated YouTube content without PO tokens
- **Output file visibility** — converted and compressed files were stored in app-private directory; now accessible via file managers
- **Download state clarity** — users can now clearly see when download button is disabled vs enabled
- **FFmpeg progress parsing** — removed duplicate code, now uses shared FfmpegProgressParser

### Technical
- Added `isDownloadButtonDisabled`, `downloadButtonDisabledAt`, and tracking fields in FormatUiState
- Added `clearCache()` and `getCacheSize()` methods in FileUtils
- Added CacheCard composable in SettingsScreen
- Created new .github/workflows/cleanup.yml for artifact management
- Updated future-plan.md with embedded terminal, YouTube DASH, and app size optimization roadmap

---

## [1.3.0] — 2026-04-06

### Added
- **Progress indicator during URL analysis** — a `LinearProgressIndicator` bar appears while yt-dlp fetches video info so users get visual feedback that something is happening
- **Stable signing architecture hooks** — Gradle and GitHub Actions can now consume persistent debug/release keystores so APKs can continue installing as updates instead of conflicting with existing installs

### Changed
- **YouTube download quality locked to 360p** — YouTube downloads without a manual format selection are capped at 360p, skipping the previous multi-resolution fallback chain that always ended up at 360p anyway
- **YouTube extraction simplified** — removed the 4-client extractor retry loop (default → android,web,ios,tv → web → android). A single default yt-dlp call is used instead, cutting down unnecessary network retries

### Fixed
- **APK update conflict** — same `applicationId` (`com.localdownloader`) retained across builds with incremented `versionCode` so new APKs install over old ones without requiring manual uninstall first

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