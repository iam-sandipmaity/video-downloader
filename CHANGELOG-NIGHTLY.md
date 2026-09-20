# Nightly Changelog

Nightly builds are rolling prereleases published from the `nightly` release tag. This file tracks changes that are available in nightly before they are promoted into the stable changelog.

## [2.0.2.8] - 2026-09-21

### Added
- **Playlist & Album Batch Download in Quick Share Overlay** - Extended the Quick Download share bottom sheet to seamlessly handle full playlists and albums. Shows playlist metadata with `PLAYLIST` badge, individual track/video items with thumbnails, durations, and indices, with "Select all" / "Deselect all" controls and batch queue generation.
- **Quick Download Design Documentation** - Added comprehensive architecture and UX documentation at `docs/QUICK_DOWNLOAD_POPUP_DESIGN.md`.

### Fixed
- **Android Lint & Jetpack Compose Cleanups** - Hoisted dynamic string resource queries to eliminate `LocalContextGetResourceValueCall` lint errors, corrected Composable `modifier` parameter ordering according to Compose API guidelines, and converted state to `mutableIntStateOf` to avoid autoboxing overhead.
- **Modern KTX Extensions & Cleanups** - Replaced direct `Uri.parse(...)` and `prefs.edit()` calls across ViewModels and screens with idiomatic `toUri()` and `edit {}` KTX extension functions, and removed obsolete `Build.VERSION.SDK_INT < M` checks.

### Changed
- **Nightly Version Bump** - Release metadata updated to `2.0.2.8` (`NIGHTLY_VERSION_CODE` `52`).

## [2.0.2.7] - 2026-09-21

### Added
- **Battery & Performance Settings Hub** - Added dedicated Battery & Performance settings screen with configurable Battery Saver mode (`Off`, `Auto`, `Always On`), low battery pause threshold (5%–35%), customizable fragment download threads (1–16), and power-aware scheduling.
- **Power-Aware Download Constraints & Queue Pause Banners** - Added support for "Download only while charging" and "Pause downloads on low battery". When power constraints hold or pause downloads, an informative live status banner in the Download Queue explains the condition with a direct shortcut to Battery Settings, and queued tasks display clear status indicators.
- **Android OS Battery Optimization Management** - Integrated Android OS battery optimization status tracking and deep-links to request unrestricted background execution or configure system battery profiles directly.

### Changed
- **Refined Preference Navigation & Dialog Affordances** - Differentiated subpage navigation rows (`PreferenceNavigationRow`, showing `>`) from dialog picker and action rows (`PreferenceRow`, displaying clean values without misleading navigation chevrons) across all settings screens.
- **Nightly Version Bump** - Release metadata updated to `2.0.2.7` (`NIGHTLY_VERSION_CODE` `51`).

## [2.0.2.6] - 2026-09-20

### Fixed
- **FFmpeg Update Tracking & Clean Version Display** - Fixed FFmpeg update availability comparison against bundled runtime and simplified version display labels to show clean version strings (e.g. `7.1.2`) instead of bracketed package tags.

### Changed
- **Nightly Version Bump** - Release metadata updated to `2.0.2.6` (`NIGHTLY_VERSION_CODE` `50`).

## [2.0.2.5] - 2026-09-20

### Fixed
- **Invalid Merge Output Format for Audio & Share Popups** (#108) - Resolved an issue where selecting audio streams or sharing links with non-container formats passed `--merge-output-format m4a` to `yt-dlp`. Added strict merge container validation ensuring only valid container formats (`mp4`, `mkv`, `webm`, etc.) are passed.
- **Long Unicode Filenames & Filesystem Errors** - Fixed `Errno 2: No such file or directory` / `File name too long` errors on platforms like Twitter/X with mathematical bold Unicode titles by adding `--windows-filenames` and `--trim-filenames 160` to yt-dlp arguments and ensuring target parent directories exist prior to download.
- **Thumbnail Write Failure Recovery** - Added resilient fallback retry logic in `DownloadWorker` when thumbnail writing fails during the initial download phase, falling back to seamless poster-frame generation using Android native `MediaMetadataRetriever`.

### Changed
- **Nightly Version Bump** - Release metadata updated to `2.0.2.5` (`NIGHTLY_VERSION_CODE` `49`).

## [2.0.2.4] - 2026-09-20

### Added
- **Modern Quick Download Share Sheet Overlay** (#98) - Redesigned the quick download link share flow into a modern Material 3 Modal Bottom Sheet with rich media artwork, floating duration badge (`03:45`), 2-line title, uploader/domain tags, and an inline title edit modal.
- **Immediate Categorized Audio & Video Grids** - Replaced nested menus with direct 2-column selectable cards for Music/Audio (MP3 320k HQ, 128k, M4A, FLAC) and Video (4K, 1080p 60fps, 720p HD, 480p, 360p) with live container badges, HQ pills, and estimated file sizes.
- **Collapsible Quick Tuning Drawer** - Added an expandable quick settings row to adjust fragment download threads (`[-] 4 [+]`, 1–16) and container format overrides without cluttering the screen.
- **Dynamic Contextual CTA Action Bar** - Sticky bottom download button dynamically updates with the selected quality and size (e.g. `Download 1080p (~48.2 MB)` or `Download MP3 320k (~8.4 MB)`).
- **Shimmer Skeleton Loader** - Added animated shimmer placeholders for the header and format cards during link extraction to prevent layout shifts.

### Changed
- **Nightly Version Bump** - Release metadata updated to `2.0.2.4` (`NIGHTLY_VERSION_CODE` `48`).

## [2.0.2.3] - 2026-09-20

### Added
- **Startup Update Availability Popup** (#105) - Automatically checks for App, yt-dlp, and FFmpeg updates on app launch and displays a unified startup update dialog when new updates are available.
- **Update Remind Later & Version Dismissal** - Supported temporary deferral ("Remind Later" with cooldown period) and skipping specific versions ("Don't remind for this version") to avoid repetitive prompts.
- **Check Updates on Launch Preference** - Added a toggle switch in the Updates screen settings to enable or disable automatic update checks upon app startup.

### Changed
- **Nightly Version Bump** - Release metadata updated to `2.0.2.3` (`NIGHTLY_VERSION_CODE` `47`).

## [2.0.2.2] - 2026-09-16

### Changed
- **Nightly Launcher Branding Revamp** - Refreshed and aligned the nightly launcher icon and SVG brand assets. Removed the moon backdrop for a cleaner, modern look, corrected the `NIGHTLY` badge text alignment and kerning (balanced `L` and `Y` spacing), and centered the badge symmetrically within the adaptive icon safe zone.
- **Nightly Version Bump** - Release metadata updated to `2.0.2.2` (`NIGHTLY_VERSION_CODE` `46`).

## [2.0.2.1] - 2026-09-15

### Added
- **Modern Format Selection Modal Sheet & Selector Style Setting** (#99) - Introduced a modern Material 3 bottom sheet format selector (`FormatSelectionBottomSheet` & `QuickOptionBottomSheet`) with visual quality badges (4K, 1080p, 720p, Audio), Best/Recommended stream indicator tags, FPS pills, container badges, codec and bitrate metadata, exact/estimated size highlights, and interactive stream-type filter chips (Video + Audio, Video Only, Audio Only).
- **Quick Download Share Popup Format Modal** - Applied the modern format and quality bottom sheet selection flow to the quick download popup activity when sharing links from YouTube and external apps.
- **Dynamic Format-Based Real Data Quality Filtering for Share Popup** - Share link quick download popup now parses and displays only the real formats and qualities actually fetched from the shared media link (e.g. social media videos with only 720p/1080p MP4), and dynamically filters available qualities to match the currently selected format.
- **Format Selector Presentation Style preference** - Added a setting in Download Settings allowing users to choose between the modern **Modal Sheet (Visual Badges)** and the traditional **Compact Dropdown** format menu.

## [2.0.2.0] - 2026-09-15

### Added
- **Quality & format details settings** (#99) - Added toggles in Download Settings to customize media information displayed in download cards and format selectors (Show FPS, Show Codecs, Show Bitrate).
- **Clean download card format list layout** - Formatted quality choices with clear spacing, rounded card styling, highlighted selection state, and two-column alignment (resolution/label on left, file size on right).
- **Spaced and uncluttered popup menu quality options** - Added distinct spacing, rounded items, and aligned size indicators across the download options sheet and quick download popup card.

## [2.0.1.9] - 2026-09-14

### Added
- **Popup menu download for share link** (#98) - Added `QuickDownloadActivity` as a translucent overlay activity when sharing links from YouTube or social media apps.
- **Quick download UI popup** - Added floating popup card with editable title, 2-way Video/Audio toggle, Quality selector with file size estimation, Format selector (MP4, WebM, MKV, Auto / MP3, M4A, Opus, FLAC, WAV, AAC), and concurrent fragment download threads stepper (1–16).
- **Video + Audio merging & container support** - Seamlessly synthesizes and merges best video and audio streams into user-selected container formats (MP4, WebM, MKV) when downloading videos from the quick popup.
- **Multi-threaded fragment downloads** - Added `concurrentFragments` option to `DownloadOptions` and configured `DownloadEngine` to run with user-configured thread count (1–16).
- **Metered network confirmation dialog** - Prompts users on cellular connections when metered downloads are disabled in settings to avoid queued tasks stalling.
- **Safe playlist queueing** - Preserves playlist indices and subfolder structures to prevent items from overwriting each other.

## [2.0.1.8] - 2026-07-30

### Changed
- **Nightly build workflows** - Enabled Android compilation check and nightly release workflows in GitHub Actions (`master.yml`).
- **Nightly version bump** - Updated the release metadata version to `2.0.1.8` (NIGHTLY_VERSION_CODE `42`).

### Added
- **Cancel All Confirmation Dialog** - Added a confirmation popup when selecting the "Cancel All" batch option in the progress queue to prevent accidental cancellations.
- **PIN validation and helper text** - Strengthened the vault PIN setup UX with explicit mismatch/too-short messages, keyboard layout limits, and a disabled state for the save button until a valid 4–8 digit PIN matches.
- **File accessibility improvements** - Added direct tap-to-play support for completed media files in the downloads queue, and intercepted in-app play attempts on non-playable files in the vault with a Toast message.

### Fixed
- **Harden Vault Transactions** - Rewrote the file moving pipeline (`moveToVault` and `moveFromVault`) to run atomically as a `Result`. Sidecar files (subtitles, thumbnails, info metadata) are resolved and moved together. On failure, transaction state is automatically rolled back.
- **Room Migration Schema Mismatch** - Hardened Room migration 4→5 to specify `is_in_vault` as `NOT NULL DEFAULT 0` and aligned `DownloadTaskEntity` column configurations to resolve database creation and upgrade crashes.
- **Vault Session Leakage** - Enforced lock-on-exit by automatically locking the vault and canceling pending unlocks when navigating away from vault-related screens or pressing the back button.
- **Infinite video backstack growth** - Pop the current player composable route when navigating to the previous or next track, resolving nested backstack allocation and memory leaks.
- **Player missing-file lockup** - Centered a localized warning message and added a "Back" button on the video player screen when loading a missing or unplayable file, avoiding black screen lockups.
- **Empty playlist analyze guards** - Guarded URL analysis and playlist queue actions from running on empty inputs.
- **Compose Cooldown Lag** - Replaced direct epoch-time checks for the download button cooldown with a coroutine-based enable job in `FormatViewModel`, resolving UI lag and recomposition stutter.

## [2.0.1.7] - 2026-07-11

### Changed
- **Minified Nightly Builds** - enabled minification and resource shrinking for the nightly build type to align packaging with stable release builds, ensuring ProGuard/obfuscation issues are caught early during prerelease testing.
- **Nightly version bump** - release metadata updated to `2.0.1.7`.

### Fixed
- **yt-dlp-android ProGuard keeps** - added rules to keep `com.yausername.youtubedl_android` and `com.yausername.youtubedl` classes. This resolves the `rxo` (obfuscated package name) runtime crash and the "unknown" version label in minified builds.
- **Apache Commons Compress & XZ keeps** - added rules to keep `org.apache.commons.compress` and `org.tukaani.xz` classes, preventing ClassNotFoundExceptions like `ExtraFieldUtils` when extracting zipped packages.
- **Private Vault Music Isolation** - isolated the music player layout when starting playback from the secure vault. The player now disables the interactive source selector and hides non-vault media entries (device or downloads) to prevent private file metadata leakage.

## [2.0.1.6] - 2026-07-05

### Changed
- **Weblate translation fallback policy** - partial Weblate translation updates no longer fail lint when strings are still untranslated. Android will use the default English strings for missing localized entries.
- **Language catalog alignment** - registered every available app locale consistently across Android locale config and the in-app language picker: English plus Bengali, German, Spanish, French, Hindi, Japanese, Kannada, Korean, Malayalam, Dutch, Russian, Tamil, Telugu, and Simplified Chinese.
- **Nightly version bump** - release metadata updated to `2.0.1.6`.

## [2.0.1.5] - 2026-07-02

### Added
- **Private Vault** - Secure download storage with PIN protection. Completed downloads can be moved to a private vault that is not backed up to cloud storage.
- **Multiple Vaults** - Support for creating and managing multiple vaults (e.g. Work, Personal) with distinct PIN credentials and secure subfolder allocations.
- **Auto-Move URL Rules** - Vault settings allowing users to add URL prefixes so matching downloads are automatically secured in the selected vault upon completion.
- **Vault Tab Filters and Search** - Added tab filters (All, Videos, Audios, Others) and a full-text Search Bar to easily manage secure vault items.
- **In-App Music Player Integration** - Audio files in the vault now open directly in the full-screen music player, building a secure queue of all audio tracks in that vault.
- **Click-to-Play** - Made vault item cards clickable to play secure video and audio files seamlessly inside the app.
- **Redirect Setup Prompt** - Prompt dialog offering setup navigation if the user attempts to secure files from downloads but has not created any vault yet.

### Fixed
- **Vault serialization** - Fixed "Serializer for class 'VaultSettings' is not found" error by adding `@Serializable` annotation.
- **State Reactivity** - Replaced direct StateFlow value access in Compose screens with reactive state collection to guarantee instant recomposition.
- **Security & Privacy Leak** - Changed file moving logic to delete staging copies from public MediaStore when securing files, hiding them completely from other apps.
- **MediaStore Export on Move-Out** - Re-export files to the public Downloads folder when moved out of the vault so they become visible to system file manager apps again.

## [2.0.1.4] - 2026-07-01

### Fixed
- **Restored Official youtubedl-android Dependency** - Replaced the custom Python and QuickJS executable runtime pipeline with the official `io.github.junkfood02.youtubedl-android:library:0.18.1` dependency wrapper. This successfully resolves all startup tracebacks, platform execution permissions, and architecture mismatches while restoring the stable performance of version `2.0.1.1`.
- **FFmpeg package updates** - Updated custom FFmpeg package compilation configuration to enable WebP/GIF demuxers, muxers, and decoders, as well as subtitle encoders and handlers. This fixes missing/incorrect video/audio thumbnails and subtitle embedding failures.
- **Fail-Fast Extractor Loop** - Added fatal system error checks inside `FormatExtractor` to immediately abort the candidate extractor loop on subprocess crashes or linker errors, preventing the main thread from hanging on broken runtimes.
- **CI Build Pipeline Rate-Limiting** - Replaced dynamic GitHub REST API calls in gradle config tasks with a static release asset download URL to avoid unauthenticated HTTP 403 rate-limit blocks on CI environments.

## [2.0.1.3] - 2026-06-29

### Fixed
- **Python Hashing/Cryptography Modules** - Compiled standard hashing modules (`_md5`, `_sha1`, `_sha256`, `_sha512`, `_sha3`, and `_blake2`) statically into the Python runtime. This resolves `ValueError: unsupported hash type blake2b` errors during startup and restores YouTube signature deciphering functionality.
- **UI Performance and Lag** - Added a time-based throttle (250ms interval) to download progress updates in `DownloadEngine`. This prevents rapid terminal output (e.g. during HLS fragment downloads) from flooding the Main thread with excessive Jetpack Compose recompositions, eliminating UI freezes and progress bar hangs.

## [2.0.1.2] - 2026-06-29

### Added
- **Custom Python Runtime Integration** - Integrated a custom precompiled Python 3.11.9 runtime binary. To prevent dynamic linker namespace crashes on Android 10+, the entire CPython engine (including standard C extensions such as `_ssl`, `_socket`, `_ctypes`, and `zlib`) is compiled into a completely static, standalone executable (`libpython.so`).
- **Python Build Automation** - Configured the Gradle build process to dynamically download, extract, and bundle our custom-compiled static Python executable (`libpython.so` and standard library ZIP `libpython.zip.so`) from the packages repository during build time, eliminating the `libpython3.11.so` dependency.
- **Python Standard Library Redirection** - Added `PYTHONPATH` redirection in `YtDlpExecutor` pointing directly to our `libpython.zip.so` to force the runtime to load Python 3.11.9 standard library bytecode, preventing `ImportError` magic number mismatches.
- **Custom QuickJS Engine Integration** - Integrated custom precompiled QuickJS runtime binaries to override the external wrapper's embedded engine.
- **QuickJS Build Automation** - Configured the Gradle build process to dynamically download and bundle our custom-compiled QuickJS shared library (`libqjs.so`) from the packages repository during compilation.
- **Dynamic Binary Packaging** - Ignored the downloaded Python and QuickJS binaries in Git and automated local packaging to ensure a clean codebase.

## [2.0.1.1] - 2026-06-28

### Changed
- **FFmpeg Update Path Resolution** - Relaxed release tag verification to search for the `"ffmpeg"` keyword inside the release APK asset names. This allows standard version-only release tags (e.g. `v7.0.1`) to resolve correctly on all devices.
- **Dynamic Version Display** - Resolved a bug where failed update checks caused the Updates screen to report the current version as `"unknown"`. The screen now dynamically displays the actual running bundled or installed FFmpeg version even when the network check fails.
- **Master CI Workflow Toggles** - Enabled CodeQL scanning and Android build compilation checks in CI/CD pipeline triggers.
- **FFmpeg Build Automation** - Configured the Gradle build process to automatically fetch, extract, and bundle the latest precompiled FFmpeg binaries from the release repository during compilation, removing the need to track large binaries in the codebase.
- **FFmpeg Dependency Refactoring** - Shifted from using an external precompiled FFmpeg dependency to our own precompiled FFmpeg binaries, optimizing the build and ensuring full control over the compiled binary.
- **Workflow & Installer Migration** - Updated CI/CD validation steps and BinaryInstaller background cleanup logic to transition fully to the new binary format (`libffmpeg.so` replacing the legacy `libffmpeg_exec.so`).

## [2.0.1.0] - 2026-06-28

### Changed
- **FFmpeg Custom Package Repository** - Updated the update manager to fetch from `iam-sandipmaity/video-downloader-packages` instead of the placeholder repository.
- **Custom Signature Fingerprint** - Added the custom release signature certificate fingerprint to trusted digests so in-app FFmpeg updates can verify and install correctly.

## [2.0.0.2] - 2026-06-06

### Added
- **Nightly launcher branding** - nightly builds now use a separate orange-purple launcher icon with a small `NIGHTLY` badge so they are easier to tell apart from stable installs.

### Changed
- **Nightly version bump** - release metadata updated to `2.0.0.2`.

## [2.0.0.1] - 2026-06-05

### Added
- **Queue reordering** - waiting queue items can now be moved earlier or later before they are assigned to a worker slot.
- **Troubleshooting report** - the app log screen can export a sanitized report with app version, release channel, Android/device details, runtime status hints, and the latest failed command summary.
- **Storage visibility** - storage settings now show available device storage and a possible duplicate saved-items count.
- **First-run setup actions** - the setup sheet now links directly to download folder setup, default format settings, cookies, and YouTube access setup.
- **Path copy actions** - task diagnostics can copy the saved file path or containing folder path when Android exposes a local path.
- **Audio player shortcut in More** - the music player is now launched from the More tools section, before Converter, instead of being promoted as a standalone Downloads-tab banner.
- **Real audio source picker** - the music player can switch between app downloads, device audio from MediaStore, and a persisted user-selected folder.
- **Device and folder playback support** - content URI audio from MediaStore and Android folder picker sources can now play, show thumbnails, share, and populate notification metadata without requiring a direct filesystem path.
- **Embedded audio metadata** - app-downloaded and folder-selected audio now read embedded title, artist, album artist, album, and duration tags for player headers and details.
- **Expanded player actions** - added dedicated detail, rename, trim, sleep timer, share, and set-as flows, while keeping app-library-only actions scoped to app-downloaded tracks.
- **Audio trim editor** - trimming now uses its own start/end editor and exports a selected range as a new audio file instead of reusing A-B loop points.
- **External audio trimming** - device-audio and selected-folder tracks can now use the trim editor, including content URI sources.
- **A-B loop markers** - selected A and B loop points are shown on the progress bar with the looped span highlighted.
- **Video player shortcut in More** - added a Video player entry before Audio player with downloaded-video playback, device video browsing, and a gesture guide.
- **Video gesture guide** - first video playback now introduces brightness, volume, seek, zoom, and pan gestures, and the same guide can be reopened from More.
- **Two-finger video zoom** - the in-app video player now supports standard pinch-to-zoom and two-finger pan gestures without blocking single-finger seek, brightness, or volume gestures.
- **Cleaner gesture hint** - refreshed the video gesture guide with a cinematic preview card, clearer labels, and drawn touch markers.

### Changed
- **Queue diagnostics** - task details now show progress, speed, ETA, transferred size, source, output path, and recent log lines in one diagnostics panel.
- **Update channel clarity** - the Updates screen now shows the active app release channel and explains stable/nightly routing.
- **Music player redesign** - replaced the old in-app music player surface with a full-screen deck, animated vinyl-style artwork, improved tonearm layout, refreshed controls, and a compact playing queue.
- **Premium playback visuals** - the player background now blends blurred artwork with the active accent color, and tracks without artwork use a designed default music visual.
- **Playback controls behavior** - shuffle, repeat, favorite, queue, lyrics, A-B, and More actions now have clearer dedicated behavior instead of sharing the same generic options sheet.
- **Progress interaction polish** - the progress bar now supports reliable tap-to-seek and drag seeking, with cleaner styling and loop-point feedback.
- **Screenshot gallery refresh** - README media previews now include the refreshed audio player, audio options sheet, and portrait/landscape video player screenshots.
- **Weblate README widgets** - added Weblate language and status widgets, and refreshed the contributors image cache key.

### Fixed
- **Failure history clarity** - final failure and cancel reasons are now kept in the task debug history instead of being hidden behind earlier trace lines.
- **Update channel safety** - stable builds block nightly APK assets before install preparation, while nightly builds only accept nightly APK assets.
- **Seek tap regression** - tapping a new point on the progress bar now seeks to that position instead of snapping back to the previous playback second.
- **Device audio source display** - switching to device audio now shows and plays the loaded device tracks even when an older app-download queue is still active.
- **Tonearm positioning** - adjusted the gramophone/tonearm geometry so the stylus sits on the record more naturally across artwork states.
- **Music source compile issues** - fixed wiring mistakes around the More-page music shortcut and content-backed player artwork.
- **A-B marker compile issue** - fixed the loop marker state references used by the progress bar.

### Known Issues
- **Local compile not verified here** - this development machine does not have `ANDROID_HOME` or `local.properties` configured, so Kotlin compile verification could not run locally.
- **PR release publishing** - pull request builds validate the nightly code path, but the rolling `nightly` release is only published after the branch lands on `main`.

### Promoted To Stable
- Summarized in stable `1.7.4`.

## [2.0.0.0] - 2026-05-31

### Added
- **Queue reordering** - waiting queue items can now be moved earlier or later before they are assigned to a worker slot.
- **Troubleshooting report** - the app log screen can export a sanitized report with app version, release channel, Android/device details, runtime status hints, and the latest failed command summary.
- **Storage visibility** - storage settings now show available device storage and a possible duplicate saved-items count.
- **First-run setup actions** - the setup sheet now links directly to download folder setup, default format settings, cookies, and YouTube access setup.
- **Path copy actions** - task diagnostics can copy the saved file path or containing folder path when Android exposes a local path.

### Changed
- **Queue diagnostics** - task details now show progress, speed, ETA, transferred size, source, output path, and recent log lines in one diagnostics panel.
- **Update channel clarity** - the Updates screen now shows the active app release channel and explains stable/nightly routing.

### Fixed
- **Failure history clarity** - final failure and cancel reasons are now kept in the task debug history instead of being hidden behind earlier trace lines.
- **Update channel safety** - stable builds block nightly APK assets before install preparation, while nightly builds only accept nightly APK assets.

### Known Issues
- **Local compile not verified here** - this development machine does not have `ANDROID_HOME` or `local.properties` configured, so Kotlin compile verification could not run locally.
- **PR release publishing** - pull request builds validate the nightly code path, but the rolling `nightly` release is only published after the branch lands on `main`.

### Promoted To Stable
- Included in the stable `1.7.4` promotion through the `2.0.0.1` nightly summary.
