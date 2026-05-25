# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Wrapper-first builds** - local docs and GitHub Actions now use the checked-in Gradle wrapper instead of assuming a globally installed `gradle` binary

### Fixed
- **CI verification coverage** - the Android workflow now blocks on explicit Kotlin compile and unit test checks for the standard debug variant before publishing artifacts
- **Lint report visibility** - standard debug lint now runs as an advisory CI step and uploads reports while the existing lint backlog is being worked down
- **Unit test Kotlin assertions** - the app module now includes the Kotlin JUnit test bridge required by the existing `kotlin.test.*` unit tests
- **Unit test temp directory API** - split artifact tests now use `kotlin.io.path.createTempDirectory` so Kotlin 2.3 no longer fails compilation on the deprecated `createTempDir` helper
- **JVM-safe URL parsing** - URL validation and source-host detection now use Java URI parsing so standard unit tests no longer depend on Android framework URL helpers
- **MediaStore API guard** - the legacy delete path now returns early below Android 10 before touching API 29-only `MediaStore.Downloads` fields
- **Tagged release validation** - version tags now fail fast when `github.ref_name` does not match `APP_VERSION_NAME`
- **Artifact cleanup pagination** - scheduled cleanup now paginates through the full artifact list instead of only deleting from the first page
- **Cookie export feedback** - exporting cookies to a file now surfaces success and real write failures instead of failing silently when the destination stream cannot be opened

## [1.7.2.4] - 2026-05-25

### Changed
- **Safer runtime update blocking** - yt-dlp updates now wait for queued, running, or paused downloads across both manual installs and background auto-update work so runtime replacement does not race active queue state

### Fixed
- **Paused-download cleanup scope** - expired paused downloads now clean up only the app-managed artifacts for that task instead of overmatching similarly named sibling files in the same folder
- **Public export write safety** - Android 10+ public-download export now treats MediaStore stream-open failures as real failures, cleans up partial inserts, and keeps the private staging copy unless the public write actually succeeds
- **Large analyze memory pressure** - link analysis now prefers the captured info-json snapshot and caps retained stdout text so huge playlist or site responses are less likely to inflate memory usage
- **WorkManager observer cleanup** - per-task work observers are now replaced and canceled cleanly on retries, resumes, and terminal states instead of lingering after the tracked work is no longer active

### Technical
- **App version bump** - release metadata updated to `1.7.2.4`

## [1.7.2.3] - 2026-05-24

### Added
- **Library search** - Downloads and History now include inline search so saved media and past tasks are easier to filter by title, source, file name, saved path, and related diagnostics
- **External player fallback** - the built-in player can now hand off saved or imported media to another player more easily, and broader media container types are recognized across library and open-with flows
- **Repo-safe update flavor** - added a `repoSafe` distribution flavor that keeps yt-dlp auto-update off by default for repository-friendly builds while preserving the standard flavor behavior

### Changed
- **Recovery guidance classification** - failed queue items now classify likely problems into clearer buckets such as access restrictions, session issues, network or rate-limit trouble, FFmpeg post-processing failures, runtime or device problems, and extractor or format compatibility issues
- **Downloads header density** - compacted selection actions plus top filter and sort controls so batch actions fit more cleanly in one row with less wasted vertical space
- **Playback-safe video defaults** - when no exact format is chosen manually, video downloads now prefer more device-friendly defaults instead of automatically drifting to the tallest risky stream
- **Brief compatibility heads-up** - variable-support formats like `WEBM` and `MKV` now show only a short 2-second playback notice when the player opens instead of a persistent warning

### Fixed
- **Targeted failure help** - recovery panels now surface more useful next steps and actions for common failure patterns instead of falling back to one generic troubleshooting block
- **Audio extraction recovery** - `WAV` and `FLAC` conversions no longer receive invalid bitrate arguments, and post-processing failures now recover already-downloaded source audio more reliably
- **FFmpeg runtime preference** - runtime setup now tries the newer linked or overlay FFmpeg before falling back to the legacy bundled binary, improving compatibility with updated runtimes
- **Player format support plumbing** - more audio and video containers now get correct MIME hints, library classification, and in-app or external-open handling instead of being treated inconsistently

### Technical
- **App version bump** - release metadata updated to `1.7.2.3`

## [1.7.2.2] - 2026-05-24

### Added
- **Derived output transforms for muxed-only sources** - muxed-only links can now expose optional `Extract audio` and `Remove audio` actions without pretending those stream types exist natively on the source site

### Changed
- **Truthful format tabs** - source tabs now stay aligned to the real streams returned by yt-dlp, so audio-only sites remain audio-only, mixed sites like X keep their real combinations, and muxed-only sources no longer light up unavailable native tabs
- **Output-aware format sheet behavior** - filename templates, subtitle toggles, and related extras now follow the effective output type when a derived transform is chosen instead of assuming the original source tab tells the whole story

### Fixed
- **Muxed-source postprocessing** - selecting `Remove audio` now performs a real ffmpeg strip-audio pass after download instead of leaving the completed file muxed
- **Release shrinker dependency gap** - release builds now include the missing `org.tukaani:xz` runtime required by the bundled `commons-compress` path, fixing the `release-main` R8 failure on `org.tukaani.xz.*` classes

### Technical
- **App version bump** - release metadata updated to `1.7.2.2`

## [1.7.2.1] - 2026-05-24

### Added
- **GitHub wiki starter set** - added a publish-ready wiki content pack covering setup, download workflow, playback, troubleshooting, updates, architecture, development, and policy pages

### Changed
- **Auto container defaults** - single-file video downloads now default to `Auto` container resolution, and the picker surfaces the safer final output container more honestly instead of always implying `mp4`
- **Explicit MP4 safety filtering** - choosing `mp4` now prefers MP4-safe video-plus-audio combinations and keeps codec intent clearer for AVC1, AV1, and VP9-family picks
- **Container-routing polish** - YouTube merge routing now chooses cleaner final targets for AVC1, AV1, and VP9/Opus selections before worker recovery needs to step in
- **README presentation refresh** - reorganized the repository landing page into a more visual release overview with categorized screenshot tables so every current demo image stays visible but easier to scan
- **Preview gallery consistency** - README and wiki screenshot galleries now use a more stable two-column rhythm for larger sections so GitHub renders them at a more even visual size
- **Nightly publishing control** - standard CI runs now keep building APK artifacts without auto-publishing GitHub nightly prereleases, and nightly release creation is now an explicit manual workflow choice

### Fixed
- **Non-YouTube analysis timeout recovery** - slower sites now get safe extended-timeout retries during link analysis without restoring the old insecure certificate-bypass fallback
- **yt-dlp runtime updater flow** - the app now routes yt-dlp updates through the downloader's own self-update path instead of manually replacing the embedded runtime file, improving real extractor compatibility recovery
- **Audio extraction merge flags** - audio-only downloads no longer pass invalid yt-dlp merge-output arguments like `m4a`, restoring direct `Audio only` downloads on sites that expose standalone audio formats
- **Audio-only source classification** - links that only expose audio containers, including JioSaavn-style `m4a` sources, now stay under `Audio only` instead of being mislabeled as `Video + audio`, and unavailable video tabs are disabled
- **Site compatibility regressions after 1.7.2.0** - restored newer extractor/runtime recovery behavior for pages that had started failing or exposing fewer formats after the security release, including tougher generic-site and article-video cases
- **Postprocessing recovery loops** - optional yt-dlp postprocessing failures now salvage already-finished media more reliably instead of cascading into unnecessary redownload attempts
- **VP9/WebM merge behavior** - VP9 plus Opus downloads now resolve straight to `webm` when appropriate instead of first attempting a known-fragile `mp4` merge path
- **History trace visibility** - restored sanitized per-task debug traces in Download History so completed and failed tasks remain inspectable again
- **Recovered thumbnail sidecars** - recovery paths no longer export stray thumbnail `.webp` files into public Downloads when thumbnail embedding extras cannot be completed
- **In-app video playback handoff** - opening a video now pauses background audio playback from the app, and the center play/pause overlay is visually centered again
- **Android chooser overmatching** - the app no longer advertises itself for unrelated browser and file-open targets, while still supporting shared links plus audio and video `Open with` flows

### Technical
- **App version bump** - release metadata updated to `1.7.2.1`

## [1.7.2.0] - 2026-05-23

### Changed
- **Security hardening release** - bundled the vulnerability-fix pass into one follow-up release covering update trust, storage handling, WebView safety, and diagnostic-data retention
- **Scoped storage alignment** - Android 10 now follows the same app-specific download staging plus public-export flow as newer Android versions, so legacy external-storage behavior is no longer required

### Fixed
- **TLS downgrade during analysis** - removed the insecure certificate-check bypass from the analysis fallback path
- **Sensitive local data exposure** - moved download secrets out of task persistence, cleared terminal task traces, disabled Android backup for app data, and tightened log/export handling
- **Runtime update verification gaps** - app and FFmpeg updates now verify signer identity, while yt-dlp updates now require and validate the published checksum manifest before replacement
- **WebView trust boundaries** - hardened preview, cookie-capture, and YouTube access WebViews, and retired the in-app PO-token generator in favor of safer saved session hints
- **File-sharing surface area** - narrowed `FileProvider` cache exposure to the exact update and export directories the app actually shares

### Technical
- **App version bump** - release metadata updated to `1.7.2.0`

## [1.7.2] - 2026-05-22

### Added
- **Seal-style settings architecture** - replaced the one-page settings wall with a dedicated settings hub plus focused pages for Appearance, Download defaults, Folders and storage, Notifications, Access and network, and About and support
- **Concurrent download preference UI** - surfaced the stored `maxConcurrentDownloads` setting in the new Download defaults page so queue-slot intent is now visible in the UI ahead of deeper scheduler work
- **Shared preference-page system** - introduced reusable large-app-bar settings scaffolds, grouped preference rows, hero cards, and shared dialogs to keep revamp work consistent across settings-related screens
- **Android app-language handling** - added system-aware app language support so the app can follow Android's language setting cleanly and grow into per-app language selection later
- **Broader Android locale integration** - Appearance now links into Android's own app-language picker, syncs external language changes back into app settings, and exposes a wider list of supported locale tags
- **Real download network controls** - added Wi-Fi-only downloading by default, a cellular-download setting, and an in-app prompt when someone tries to queue downloads on mobile data
- **Real queue slot scheduling** - wired the concurrent-download preference into the repository scheduler so the app now respects the configured maximum active downloads
- **Downloads batch selection mode** - the Downloads library now has a `Select` action so multiple saved files can be picked together and then shared, removed from the app, or permanently deleted in one go
- **Settings app-log reader** - added an in-app `App log reader` screen under Settings so internal `app.log` lines can be filtered by outcome or day, then copied or exported without leaving the app
- **App-log backup controls** - the App log reader now includes device-backup controls, a manual `Back up now` action, and internal log-retention settings for keeping rotated history under control
- **Analyzed link ready history** - Home can now keep analyzed links stacked as reusable ready cards, with a Download-defaults setting for persistence and retention days across app restarts
- **Repository standards files** - added `LICENSE`, `CODE_OF_CONDUCT.md`, and `CONTRIBUTING.md` so the project now ships with clear licensing, contribution guidance, and collaboration expectations

### Changed
- **Settings revamp** - rebuilt Settings around a lighter Seal-inspired preference flow with grouped sections, cleaner navigation, smoother page rhythm, and clearer summaries of each category before you tap in
- **More page redesign** - turned More into a cleaner grouped utility center so workflow shortcuts, access tools, updates, help, and media utilities feel more intentional and easier to scan
- **Support surface refresh** - aligned Help and Updates with the new preference-page layout so support, maintenance, and runtime management feel like part of the same UI family
- **Browse sheet polish** - simplified Browse back to the main input flow and upgraded the download-options sheet with a tighter media header, card-style pickers, stronger selected-format highlights, cleaner playlist rows, and a more polished sticky action area
- **Access screen polish** - refreshed Cookies and YouTube access with smoother large-app-bar treatment and cleaner entry points from the new Access and network settings page
- **App-log viewer revamp** - rebuilt the App log reader into a tighter log-style screen with minimal filters, lighter maintenance controls, and a cleaner monospace output stream instead of the older settings-heavy layout
- **Playlist download controls** - playlists now expose a real global format section plus a file-wise format section where each item stays visible, can be selected individually, and can override the shared format choice when needed
- **Playlist sheet previews and naming** - the browse-sheet playlist picker now shows item thumbnails and lets each queued file keep the source title or be renamed before download
- **About and support credits** - refreshed the About page with the updated LinkedIn profile and a new credits section linking the app's open-source stack to their official sites or upstream repositories
- **Pre-download filename editing** - single downloads now let people adjust the final file name right inside the format sheet while still defaulting to the source title when left unchanged
- **Media default polish** - embedded thumbnails are now enabled by default so saved downloads keep their source artwork unless a user turns that off
- **Localization rollout, phase 1** - trimmed app-language choices down to the currently supported set (`System default`, `English`, `Bengali`, `Hindi`), removed the redundant Android language-settings shortcut, and moved the More plus core Settings pages onto real translated string resources
- **Localization rollout, phase 2** - extended translated resources into the queue, history, updates, help, converter, compressor, cookies, YouTube access, and changelog surfaces while intentionally leaving the raw App log reader output untranslated
- **Localization rollout, phase 3** - filled the remaining Hindi and Bengali UI-resource gaps so the current base string set now has full locale coverage without falling back to English on queue, history, updates, help, converter, compressor, cookies, or YouTube access screens
- **Localization rollout, phase 4** - added Tamil, Telugu, Kannada, Malayalam, Korean, Japanese, and Simplified Chinese to the app-language picker and Android locale config, seeded from the current English resource set so those languages can be completed incrementally without touching app logic
- **Localization rollout, phase 5** - completed full resource coverage for Tamil, Telugu, Kannada, Malayalam, Korean, Japanese, and Simplified Chinese so those app-language options now map to real translated string sets instead of English-seeded placeholders
- **Downloads card metadata polish** - saved files now surface format, quality, size, and fresher relative date labels like `today` or `yesterday` without changing the overall card style
- **Recovery copy cleanup** - updated onboarding and queue guidance so cookie and YouTube access directions point to the new Settings access path instead of the older More-only flow
- **Typography tune-up** - expanded the app typography set so the new settings and support pages can use cleaner headline, label, and small-body styling without falling back to mismatched defaults
- **Converter and compressor refresh** - simplified both media-tool screens around the same compact top-bar and grouped-card rhythm as Settings, with less filler copy and faster access to the useful controls
- **More page cleanup** - removed the inactive support-posture row so every entry in More now leads somewhere useful
- **Updates center clarity** - app updates now keep the downloaded APK ready across the unknown-sources permission handoff, FFmpeg first-time runtime installs are separated from real runtime updates, and install actions only appear when they are actually useful
- **Repository standards refresh** - refreshed the core markdown set so README, audit notes, implementation notes, compatibility guidance, development docs, architecture notes, and roadmap language now match the current stable `1.7.2` product posture instead of the older rapid-iteration framing
- **README preview gallery refresh** - replaced the outdated screenshot references with the current `public/demo` gallery so the repository preview now reflects the latest Home, playlist, queue, downloads, settings, access, media-tool, and localization screens

### Fixed
- **Notification toggle behavior** - completed, failed, and canceled download notifications now obey the in-app toggles again instead of depending on a broken duplicate settings path
- **False download failures after success** - fixed the completion-path notification regression that could mark a finished download as failed after the file had already been saved
- **Duplicate playlist worker launches** - fixed queue scheduling so one playlist item is no longer started twice and then forced into a rename/file-missing failure at the end
- **Queue row stability during simultaneous downloads** - running items now keep a steady visual order in the queue instead of swapping positions every time progress updates arrive
- **Bottom-sheet overscroll bounce** - download options and history log sheets now keep the drag handle responsive, keep the Browser download action fixed, and block edge drag/fling handoff where needed so those sheets no longer jitter or shake during scroll
- **Changelog rendering and sourcing** - the Updates changelog page now shows the latest app release notes first, keeps the full bundled app changelog below, and renders common markdown styling instead of dumping raw formatting markers
- **Runtime update safety gating** - updating the app, yt-dlp, or FFmpeg now blocks while downloads are queued, running, or paused so update actions do not race against active work
- **Log bloat from yt-dlp output** - analyze JSON and duplicate runtime/download line logging no longer flood `app.log`, so exported logs stay smaller and easier to inspect
- **Log backup visibility** - device log backups now write into the same user-visible Downloads area as the rest of the app instead of disappearing into an app-private external folder

### Technical
- **Navigation split for settings** - added dedicated settings subroutes inside `DownloaderApp` to support the new hub-and-subpage structure without touching download, queue, or media-processing logic
- **App version bump** - release metadata updated to `1.7.2`

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
