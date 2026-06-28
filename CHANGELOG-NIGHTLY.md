# Nightly Changelog

Nightly builds are rolling prereleases published from the `nightly` release tag. This file tracks changes that are available in nightly before they are promoted into the stable changelog.

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
