# Changelog

## [Unreleased]

## [1.7.1.0-beta] - 2026-05-21

### Added
- **Seal-style settings architecture** - replaced the one-page settings wall with a dedicated settings hub plus focused pages for Appearance, Download defaults, Folders and storage, Notifications, Access and network, and About and support
- **Concurrent download preference UI** - surfaced the stored `maxConcurrentDownloads` setting in the new Download defaults page so queue-slot intent is now visible in the UI ahead of deeper scheduler work
- **Shared preference-page system** - introduced reusable large-app-bar settings scaffolds, grouped preference rows, hero cards, and shared dialogs to keep revamp work consistent across settings-related screens
- **Android app-language handling** - added system-aware app language support so the app can follow Android's language setting cleanly and grow into per-app language selection later
- **Real download network controls** - added Wi-Fi-only downloading by default, a cellular-download setting, and an in-app prompt when someone tries to queue downloads on mobile data
- **Real queue slot scheduling** - wired the concurrent-download preference into the repository scheduler so the app now respects the configured maximum active downloads
- **Downloads batch selection mode** - the Downloads library now has a `Select` action so multiple saved files can be picked together and then shared, removed from the app, or permanently deleted in one go
- **Settings app-log reader** - added an in-app `App log reader` screen under Settings so internal `app.log` lines can be filtered by outcome or day, then copied or exported without leaving the app
- **App-log backup controls** - the App log reader now includes device-backup controls, a manual `Back up now` action, and internal log-retention settings for keeping rotated history under control

### Changed
- **Settings revamp** - rebuilt Settings around a lighter Seal-inspired preference flow with grouped sections, cleaner navigation, smoother page rhythm, and clearer summaries of each category before you tap in
- **More page redesign** - turned More into a cleaner grouped utility center so workflow shortcuts, access tools, updates, help, and media utilities feel more intentional and easier to scan
- **Support surface refresh** - aligned Help and Updates with the new preference-page layout so support, maintenance, and runtime management feel like part of the same UI family
- **Browse sheet polish** - simplified Browse back to the main input flow and upgraded the download-options sheet with a tighter media header, card-style pickers, stronger selected-format highlights, cleaner playlist rows, and a more polished sticky action area
- **Access screen polish** - refreshed Cookies and YouTube access with smoother large-app-bar treatment and cleaner entry points from the new Access and network settings page
- **Playlist download controls** - playlists now expose a real global format section plus a file-wise format section where each item stays visible, can be selected individually, and can override the shared format choice when needed
- **Playlist sheet previews and naming** - the browse-sheet playlist picker now shows item thumbnails and lets each queued file keep the source title or be renamed before download
- **About and support credits** - refreshed the About page with the updated LinkedIn profile and a new credits section linking the app's open-source stack to their official sites or upstream repositories
- **Pre-download filename editing** - single downloads now let people adjust the final file name right inside the format sheet while still defaulting to the source title when left unchanged
- **Downloads card metadata polish** - saved files now surface format, quality, size, and fresher relative date labels like `today` or `yesterday` without changing the overall card style
- **Recovery copy cleanup** - updated onboarding and queue guidance so cookie and YouTube access directions point to the new Settings access path instead of the older More-only flow
- **Typography tune-up** - expanded the app typography set so the new settings and support pages can use cleaner headline, label, and small-body styling without falling back to mismatched defaults
- **Converter and compressor refresh** - simplified both media-tool screens around the same compact top-bar and grouped-card rhythm as Settings, with less filler copy and faster access to the useful controls
- **More page cleanup** - removed the inactive support-posture row so every entry in More now leads somewhere useful
- **Updates center clarity** - app updates now keep the downloaded APK ready across the unknown-sources permission handoff, FFmpeg first-time runtime installs are separated from real runtime updates, and install actions only appear when they are actually useful

### Fixed
- **Notification toggle behavior** - completed, failed, and canceled download notifications now obey the in-app toggles again instead of depending on a broken duplicate settings path
- **False download failures after success** - fixed the completion-path notification regression that could mark a finished download as failed after the file had already been saved
- **Duplicate playlist worker launches** - fixed queue scheduling so one playlist item is no longer started twice and then forced into a rename/file-missing failure at the end
- **Queue row stability during simultaneous downloads** - running items now keep a steady visual order in the queue instead of swapping positions every time progress updates arrive
- **Bottom-sheet overscroll bounce** - download options and history log sheets now keep the drag handle responsive, keep the Browser download action fixed, and block edge drag/fling handoff where needed so those sheets no longer jitter or shake during scroll
- **Changelog rendering and sourcing** - the Updates changelog page now shows the latest app release notes first, keeps the full bundled app changelog below, and renders common markdown styling instead of dumping raw formatting markers
- **Runtime update safety gating** - updating the app, yt-dlp, or FFmpeg now blocks while downloads are queued, running, or paused so update actions do not race against active work
- **Log bloat from yt-dlp output** - analyze JSON and duplicate runtime/download line logging no longer flood `app.log`, so exported logs stay smaller and easier to inspect

### Technical
- **Navigation split for settings** - added dedicated settings subroutes inside `DownloaderApp` to support the new hub-and-subpage structure without touching download, queue, or media-processing logic
- **App version bump** - release metadata updated to `1.7.1.0-beta`

## [1.7.1] - 2026-05-20

### Added
- **First-run setup sheet** - replaced the passive Home reminder with a two-step onboarding sheet that lets new users either continue without cookies or jump straight into Cookies and YouTube access setup
- **Queue source branding** - download queue hero cards now show recognized site badges so YouTube, Instagram, TikTok, and similar sources are easier to scan at a glance
- **Smarter format estimates** - download options now show estimated final size labels when yt-dlp does not return an exact file size, using bitrate and duration as a fallback
- **Format-aware picker rows** - download options now show file size directly beside each selectable format, with richer codec, container, fps, and quality metadata in the picker itself
- **Expandable queue diagnostics** - queue items can now reveal a compact diagnostics panel with task ID, source host, output path, error details, and recent log lines
- **Failed-item batch retry** - the queue now offers a `Retry All Failed` action from the failed-items tab so recovery is not limited to one card at a time

### Changed
- **Queue screen redesign** - refreshed the queue into the newer hero-card layout with thumbnail-first previews, stronger empty states, and clearer top-level queue filtering
- **Download card previews** - queued and running items now prefer analyzed remote thumbnails before a local output file exists, so users see the real media preview instead of a generic placeholder
- **Playlist control behavior** - pausing, canceling, or resuming one playlist item no longer applies that action to the entire remaining playlist queue by accident
- **Queue batch controls** - added section-level pause/cancel actions for queued items and resume actions for scheduled items, keeping batch behavior separate from per-item controls
- **Download sheet reliability** - made the Home screen scrollable and pinned the bottom-sheet `Download` button so it stays reachable on smaller screens and taller format-detail layouts
- **Feedback card refresh** - Home, Settings, Cookies, and YouTube access now use a cleaner inline feedback card with clearer tone, easier dismissal, and better placement near the top of each screen
- **Help center redesign** - rebuilt the Help page into a stronger support hub with a visual hero, clearer troubleshooting shortcuts, better recovery guidance, and more practical reporting instructions
- **Queue recovery polish** - failed queue items now use a clearer retry action and the recovery flow stays aligned with the built-in cookies, PO generation, log export, and issue-report shortcuts
- **AGP 9 migration cleanup** - removed the temporary built-in-Kotlin, new-DSL, and Jetifier bridge flags and switched annotation processing from kapt to KSP
- **Build script cleanup** - modernized generated changelog asset wiring to avoid the older deprecated source-set call path

### Fixed
- **Cross-screen feedback leak** - Browse/Home status messages no longer show up inside Settings, Cookies, or YouTube access because transient format messages are now scoped to the screen that created them
- **Download options sheet scroll bounce** - stabilized the analyzed-format bottom sheet by removing the unstable size animation and skipping the partial sheet state, so reaching the bottom no longer triggers rapid jumpy up/down movement

### Technical
- **Logic test coverage** - added unit coverage for first-run onboarding visibility, source-site detection, format size estimation fallback, and queue diagnostics parsing

## [1.7.0.1] - 2026-05-19

### Added
- **Security and maintenance policy** - added `SECURITY.md` and configured Dependabot coverage for Gradle and GitHub Actions updates
- **BotGuard provenance notes** - documented the current LibreTube source of the shared PO-token constants and how to refresh them later
- **Video player volume boost** - added in-player `Off`, `Low`, `Medium`, and `High` loudness boost controls for quieter videos where the device audio output needs extra help

### Changed
- **Android build toolchain refresh** - migrated the project to AGP `9.2.1`, Kotlin `2.3.21`, the Compose Gradle plugin, and newer AndroidX dependency lines
- **CI compatibility updates** - refreshed GitHub Actions versions, moved CI to Gradle `9.4.1`, and raised `compileSdk` to `36` while keeping `targetSdk` at `35`
- **Hilt AGP 9 integration** - replaced the old Hilt Gradle plugin transform path with the explicit generated-base-class pattern for `DownloaderApplication`, `MainActivity`, and `AudioPlaybackService`
- **Video player interface refresh** - reshaped the video-only playback chrome around a cleaner JustPlayer-style layout with a slimmer title bar, tighter bottom control strip, and a gear-based playback settings panel
- **Documentation refresh** - aligned the audit, implementation notes, and repository docs with the in-app-only YouTube access flow and current maintenance setup
- **App version bump** - release metadata updated to `1.7.0.1`

### Removed
- **Desktop YouTube auth helper** - removed the unused Playwright and npm helper now that cookie export and PO-token generation run entirely in-app

### Technical
- **AGP 9 bridge flags** - retained temporary compatibility flags in `gradle.properties` while the remaining legacy Android DSL cleanup is still pending

## [1.7.0] - 2026-05-19

### Added
- **Unified Updates center** - added a dedicated Updates flow for app releases, `yt-dlp`, and `FFmpeg`, including one-tap checks, install actions, source selection, and release-note viewing
- **Background yt-dlp maintenance** - added startup-triggered `yt-dlp` auto-update scheduling with retry tracking so extractor fixes can land without a full APK upgrade
- **Richer download defaults** - added separate video and audio filename templates, default audio format selection, default subtitle/embed toggles, and folder browsing for Downloads subfolders
- **Bundled in-app changelog view** - packaged the project `CHANGELOG.md` into app assets so app release notes can be opened in a cleaner documentation-style screen

### Changed
- **FFmpeg runtime resolution** - the app now prefers managed FFmpeg overlay packages when available, then falls back through bundled native and copied executables more safely
- **Theme and settings customization** - expanded accent presets, added new contrast modes, and refreshed the Settings experience around folders, templates, and media defaults
- **Downloads and history workflow** - refreshed the Downloads and History screens with bulk cleanup actions, clearer metadata, and easier access to recent logs and saved paths
- **App version bump** - release metadata updated to `1.7.0`

### Fixed
- **Split-stream merge compatibility** - post-download merges now retry with compatible containers and AAC fallback when codecs cannot be written into the requested container
- **Post-processing recovery** - failed media post-processing now attempts standalone repair, preserves recoverable split artifacts, and can remux previous partial results instead of forcing a full redownload
- **Subtitle and audio output handling** - audio-only downloads now use their own output template, and subtitle download/embed defaults stay coordinated so invalid combinations are avoided

### Technical
- **New update infrastructure** - added `UpdatesViewModel`, `AppUpdateManager`, `YtDlpUpdateManager`, `FfmpegUpdateManager`, `YtDlpUpdateWorker`, and `YtDlpUpdateScheduler` to support managed runtime and app update flows

## [1.6.2] - 2026-05-18

### Added
- **Downloads workspace refresh** - added a dedicated Downloads library plus a new More section so saved media, tools, queue access, and app settings are easier to reach
- **Built-in music player** - added an audio playback experience with queue playback, shuffle, repeat, sleep timer controls, background playback, and media-style notifications
- **Cookie manager and YouTube access flow** - added saved site-cookie support plus a dedicated YouTube access screen for capturing browser sessions and generating PO-token-based recovery data

### Changed
- **Resilient YouTube request planning** - downloader analysis and format selection now use stronger selector-building and authenticated fallback planning for tougher media requests
- **Notification routing and app navigation** - active, completed, failed, and canceled download notifications now deep-link back into the relevant app screens more cleanly
- **FFmpeg runtime packaging** - ffmpeg now runs from the packaged native runtime path with bundled support files instead of relying on the older asset-binary copy
- **App version bump** - release metadata updated to `1.6.2`

### Fixed
- **Embedded process stability** - process output handling now tolerates closed-stream shutdown races better so embedded runtime failures are reported more reliably
- **Media tool validation** - conversion and compression now reject empty or oversized inputs earlier and verify that output files were actually created before reporting success
- **Playback and library integration** - local downloads, external-open routing, and audio playback state now stay coordinated more reliably across the refreshed app flow

### Removed
- **Legacy ffmpeg asset copy** - deleted the obsolete bundled asset copy of `ffmpeg` now that the packaged native runtime path is the primary launch path

### Technical
- **New runtime modules** - added `AppNotifications`, `AudioPlaybackService`, `CookieTextCodec`, `YoutubePoTokenGenerator`, `FormatSelectorBuilder`, and `YoutubeRequestPlanner` to support the new media, auth, and runtime flows

## [1.6.1-beta] - 2026-05-02

### Fixed
- **Subtitle download reliability** - subtitle-enabled downloads now request both standard and auto-generated captions, then convert saved subtitle files to `srt` for broader device and player compatibility
- **Subtitle file visibility** - downloaded subtitle sidecars are now exported to the public `Downloads/LocalDownloader` folder alongside the main media file instead of being left behind in app-private storage
- **Local subtitle playback** - the in-app player now attaches subtitle files stored next to the video so downloaded captions appear as playable subtitle tracks for offline media too
- **Split-download subtitle support** - when a video has to be downloaded as separate video/audio streams and merged locally, subtitle sidecars are now fetched for the merged output as a follow-up step

### Changed
- **App version bump** - release metadata updated to `1.6.1-beta`

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

## [1.5.1] - 2026-05-02

### Added
- **Modern player swipe controls** - swipe up or down on the left side of the player to adjust brightness, or on the right side to adjust volume, with an on-screen hint and level indicator so the gesture stays easy to discover

### Changed
- **App version bump** - release metadata updated to `1.5.1`

## [1.5.0] - 2026-05-01

### Added
- **Modern in-app player upgrade** - rebuilt the fullscreen player with double-tap seek, playback speed controls, audio and subtitle track selection, resize modes, touch lock, and clearer buffering and seek feedback
- **Playback resume support** - added stronger session-based resume handling across rotation and normal lifecycle changes with dedicated playback state management
- **Picture-in-Picture and external open flows** - added PiP playback, direct open-with support for local video and audio files, in-app GIF and image preview, and in-app HTML/MHTML/MHT preview support
- **Shared link handling** - the app now appears in Android's share sheet for links, opens shared URLs directly in Browser, and auto-fills plus analyzes shared links immediately
- **Media library cleanup controls** - added missing-file sync, remove-from-app cleanup, delete-all-media cleanup, and safer storage-deletion settings

### Changed
- **Playlist background reliability** - playlist queues now survive background execution better, keep processing item by item, and no longer collapse after a single failed entry
- **GIF and image-style download handling** - image-like downloads now avoid unnecessary post-processing so native GIF-style content finishes more safely after download
- **Player interaction polish** - controls now fade in place, paused-state overlays dismiss correctly, and pinch-to-zoom, pan, and reset-zoom support were added for video viewing
- **Browser and queue clarity** - analyzed media can now be cleared directly from Browser, and playlist items in Progress are ordered to match the real download flow
- **Quick links refresh** - several platform logos were replaced with cleaner brand-matched artwork
- **App version bump** - release metadata updated to `1.5.0`

### Fixed
- **Playlist worker failures** - fixed background playlist downloads failing while single-item downloads still worked, including cases where items failed without a clear worker error
- **Player control issues** - fixed paused-state control fading, earlier player compile regressions, and the PiP placeholder icon
- **GIF post-processing failures** - fixed GIF downloads breaking after 100% completion because invalid input data was still being pushed through post-processing
- **Library and Browser cleanup bugs** - fixed deleted media staying visible in the library and made the Browser ready-to-download state easier to dismiss
- **Link sharing visibility** - fixed the app not appearing correctly in regular Android link-sharing flows

### Technical
- **Playback architecture** - added `PlayerViewModel` and `PlaybackSessionStore` for more stable player state management
- **External preview plumbing** - added `ExternalOpenRequest` and `ExternalPreviewScreen` for external file and link handling
- **File and rendering support** - expanded `FileUtils` for imported external-open files and managed-file sync, added `coil-gif` for animated GIF rendering, and added manifest intent filters for share targets, web links, supported file opens, and MHTML/MHT preview

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4.0] - 2026-04-09

### Added
- **Download button state management** - button disables after click to prevent duplicate downloads, shows "Please wait..." with faded appearance, re-enables when user changes format/quality/type/container/audio settings, auto-re-enables after 6-second timeout
- **Cache management in Settings** - displays current cache size with clear button to free up storage
- **Enhanced Help Screen** - comprehensive documentation covering Downloads, Converter, Compressor, Navigation tabs, Settings, Troubleshooting, and About sections
- **YouTube DASH video+audio download support** - improved format selection for higher resolution downloads
- **Artifact cleanup workflow** - GitHub Actions workflow automatically deletes artifacts older than 2 days

### Changed
- **YouTube cookie authentication** - cookies are now applied when YouTube auth is enabled OR PO token is provided (previously only worked with PO token)
- **Converter output location** - converted files now copied to public Downloads/LocalDownloader folder for easy access
- **Compressor output location** - compressed files now copied to public Downloads/LocalDownloader folder for easy access
- **Help page** - completely rewritten with much more detailed information

### Fixed
- **Cookie auth bug** - cookies were not being applied for age-gated YouTube content without PO tokens
- **Output file visibility** - converted and compressed files were stored in app-private directory; now accessible via file managers
- **Download state clarity** - users can now clearly see when download button is disabled vs enabled
- **FFmpeg progress parsing** - removed duplicate code, now uses shared FfmpegProgressParser

### Technical
- Added `isDownloadButtonDisabled`, `downloadButtonDisabledAt`, and tracking fields in FormatUiState
- Added `clearCache()` and `getCacheSize()` methods in FileUtils
- Added CacheCard composable in SettingsScreen
- Created new `.github/workflows/cleanup.yml` for artifact management
- Updated `future-plan.md` with embedded terminal, YouTube DASH, and app size optimization roadmap

---

## [1.3.0] - 2026-04-06

### Added
- **Progress indicator during URL analysis** - a `LinearProgressIndicator` bar appears while yt-dlp fetches video info so users get visual feedback that something is happening
- **Stable signing architecture hooks** - Gradle and GitHub Actions can now consume persistent debug/release keystores so APKs can continue installing as updates instead of conflicting with existing installs

### Changed
- **YouTube download quality locked to 360p** - YouTube downloads without a manual format selection are capped at 360p, skipping the previous multi-resolution fallback chain that always ended up at 360p anyway
- **YouTube extraction simplified** - removed the 4-client extractor retry loop (default -> android,web,ios,tv -> web -> android). A single default yt-dlp call is used instead, cutting down unnecessary network retries

### Fixed
- **APK update conflict** - same `applicationId` (`com.localdownloader`) retained across builds with incremented `versionCode` so new APKs install over old ones without requiring manual uninstall first

---

## [1.2.0] - 2026-04-05

### Added
- **Room database** for persistent download queue and history - tasks and completed downloads survive app kills and restarts
- **Dark theme support** - toggle light/dark appearance in Settings; preference is saved and applied on next launch
- **WorkManager exponential backoff** - transient network failures and CDN 403s retry with automatic exponential backoff (starts at 10s, max retries)
- **R8 shrinking + minification** - release builds are now minified and shrunk with comprehensive ProGuard rules for Hilt, Compose, kotlinx-serialization, yt-dlp-android, and FFmpeg
- **Media scan on download completion** - downloaded files are copied to the public `/sdcard/Download/LocalDownloader/` folder on Android 11+ (via `MediaStore`) so they appear in file managers
- **JSON caching of download options** in Room for reliable resume capability after app restarts

### Changed
- **Scoped Storage compliance** - removed deprecated `Environment.getExternalStoragePublicDirectory` for Android 10 and below; downloads use the appropriate storage path for each API level
- **Removed use-case layer** - five thin pass-through use cases (AnalyzeUrl, StartDownload, ManageSettings, ObserveDownloadQueue, ConvertMedia) were replaced with direct repository calls for simpler, flatter code
- **Non-blocking task store** - in-memory `MutableStateFlow` persists to Room asynchronously, eliminating blocking suspend calls from Worker callbacks
- **Log privacy** - removed external storage log mirrors; logs now write only to internal storage (`filesDir/logs/`)

### Fixed
- Kotlin serialization compatibility - aligned `kotlinx-serialization-json` to 1.6.3 for Kotlin 1.9.24
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

## [1.1.0] - (pre-1.2.0 baseline)

Initial release with:
- Video and audio downloading via yt-dlp
- Format picker (quality, stream type, container)
- Download queue with progress tracking
- FFmpeg-based conversion and compression
- Compose UI with Material3
- Hilt DI, WorkManager, DataStore settings
