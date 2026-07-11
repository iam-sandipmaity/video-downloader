# Nightly Changelog

Nightly builds are rolling prereleases published from the `nightly` release tag. This file tracks changes that are available in nightly before they are promoted into the stable changelog.

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
