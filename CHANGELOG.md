# Changelog

## [1.6.0] - 2026-05-02

### Changed
- **Embedded yt-dlp runtime** - downloads now rely on the built-in `youtubedl-android` runtime instead of packaging separate standalone `yt-dlp` binaries in assets and `jniLibs`
- **FFmpeg-only bundled binary validation** - CI checks and compatibility docs now focus on the `ffmpeg` binary paths that are still shipped with the app
- **App version bump** - release metadata updated to `1.6.0`

### Removed
- **Standalone yt-dlp artifacts** - deleted the unused asset and native-library copies of `yt-dlp` to reduce release packaging overhead

## [1.5.7-beta] - 2026-05-02

### Fixed
- **yt-dlp launch fallback** - download commands now retry with the asset-installed `yt-dlp` binary when the packaged native executable cannot start, and still fall back to the embedded runtime if standalone execution fails
- **FFmpeg launch fallback** - conversion and compression now retry with the asset-installed `ffmpeg` binary when the packaged native executable is unavailable or not executable

### Changed
- **Runtime cleanup on startup** - redundant extracted tool artifacts are now removed in the background when packaged native binaries are present
- **ABI packaging scope** - the current beta build is now packaged for `arm64-v8a`
- **App version bump** - release metadata updated to `1.5.7-beta`

## [1.5.6] - 2026-05-02

### Fixed
- **Pause interruption handling** - worker shutdown during a user pause no longer gets misreported as a real download failure when yt-dlp streams are closed mid-read
- **Playlist pause stability** - pausing one running playlist item now pauses the remaining playlist queue instead of letting downstream items fall into canceled states
- **Pause state reconciliation** - WorkManager terminal events now preserve the pause window cleanly so paused downloads stay resumable until the 10-minute expiry

### Changed
- **App version bump** - release metadata updated to `1.5.6`

## [1.5.5] - 2026-05-02

### Fixed
- **True pause behavior** - pausing a download now stops the active worker without turning the task into an immediate cancel, keeps the same task entry for resume, and preserves partial progress for up to 10 minutes
- **Pause expiry cleanup** - paused downloads now expire after 10 minutes, show a clear expiry message, and automatically remove cached partial download artifacts

### Changed
- **Progress tab cleanup** - logs are no longer shown in the Progress page, which now focuses on queue state, resume timing, and primary actions
- **History log viewer** - full task logs are now available from History through a cleaner dedicated log dialog
- **App version bump** - release metadata updated to `1.5.5`

## [1.5.4] - 2026-05-02

### Added
- **Audio disable option** - the player audio panel now includes `None` and `Auto` options, similar to subtitles, so audio playback can be disabled directly from the player

### Fixed
- **Double-tap seek UI friction** - left and right double-tap seek no longer force the full player chrome to pop up, which removes the brief stopped-feeling interruption during skipping
- **Center double-tap action** - double-tapping the middle zone now toggles play and pause instead of being treated like a left or right seek tap

### Changed
- **App version bump** - release metadata updated to `1.5.4`

## [1.5.3] - 2026-05-02

### Fixed
- **Rotation lock behavior** - the player lock now only locks screen rotation instead of blocking playback controls, gestures, panels, and other player actions

### Added
- **Background audio playback** - music and other audio-first files can continue playing when the app moves to the background

### Changed
- **App version bump** - release metadata updated to `1.5.3`

## [1.5.2] - 2026-05-02

### Added
- **Horizontal swipe seek** - swipe left or right across the player to preview and jump backward or forward when you release, with skip distance based on swipe length and video duration

### Changed
- **Player gesture guidance** - the in-player hint now explains both horizontal seek and vertical brightness and volume swipes
- **App version bump** - release metadata updated to `1.5.2`

## [1.5.1] â€” 2026-05-02

### Added
- **Modern player swipe controls** â€” swipe up or down on the left side of the player to adjust brightness, or on the right side to adjust volume, with an on-screen hint and level indicator so the gesture stays easy to discover

### Changed
- **App version bump** â€” release metadata updated to `1.5.1`

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
