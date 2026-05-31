# Nightly Changelog

Nightly builds are rolling prereleases published from the `nightly` release tag. This file tracks changes that are available in nightly before they are promoted into the stable changelog.

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
- None yet.
