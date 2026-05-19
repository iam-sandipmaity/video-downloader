# Project Audit Report: Video Downloader (Android)

## Audit Scope

- Audit date: 2026-05-19
- Current app version in `gradle.properties`: `1.7.0.1`
- Focus: repo state after `1.7.0`, build and CI migration, in-app YouTube access, update infrastructure, and current maintenance risks

---

## Current Snapshot

Video Downloader is now a local-first Android app built around:

- Jetpack Compose UI
- Hilt-based dependency injection
- Room persistence plus DataStore-backed settings
- `youtubedl-android` runtime plus FFmpeg integration
- in-app YouTube cookie capture and PO-token generation
- app / `yt-dlp` / FFmpeg update management from a dedicated Updates flow

The biggest architectural correction since the older audit is that the project no longer depends on a desktop auth handoff. The removed `tools/youtube-auth-helper` path has been replaced by an Android-native flow using `YoutubeAuthScreen`, `WebViewCookieExporter`, and `YoutubePoTokenGenerator`.

---

## Post-1.7.0 Change Summary

The repo picked up 6 follow-up maintenance commits after `1.7.0`:

1. Added security policy and maintenance docs
2. Bumped the app to the `1.7.0.1` line
3. Applied Dependabot-driven Android, Gradle, and GitHub Actions upgrades
4. Raised `compileSdk` to `36` for the newer AndroidX dependency set
5. Removed the old Hilt Gradle plugin transform dependency for AGP 9 compatibility
6. Removed the unused desktop YouTube auth helper and its npm / Playwright maintenance surface

This is a maintenance-heavy patch line rather than a feature-heavy release line.

---

## What Is Stronger Now

### 1. YouTube access is self-contained in the app

- the app captures YouTube login state in its own WebView
- cookies are exported locally through `WebViewCookieExporter`
- PO tokens are generated on-device through `YoutubePoTokenGenerator`
- no external Node.js or desktop browser tooling is required anymore

### 2. Updates are now a real subsystem

- `GitHubReleaseClient`, `AppUpdateManager`, `YtDlpUpdateManager`, and `FfmpegUpdateManager` give the app a structured runtime-update path
- `YtDlpUpdateScheduler` and `YtDlpUpdateWorker` allow `yt-dlp` maintenance without shipping a full APK for every extractor fix
- the bundled changelog gives the update flow something useful to show in-app

### 3. Build and CI are much more current

- root build now uses AGP `9.2.1` and Kotlin `2.3.21`
- the app module moved to the Compose plugin path expected by Kotlin 2.x
- GitHub Actions versions were refreshed and CI now uses Gradle `9.4.1`
- `compileSdk` is now `36`, which matches the requirements of the upgraded AndroidX stack

### 4. Security and maintenance posture improved

- `SECURITY.md` is now present at the repo root
- Dependabot now tracks Gradle and GitHub Actions updates
- BotGuard constant provenance is documented in code so future refreshes are easier to trace

### 5. The repo is no longer test-free

There is still only light coverage, but the repo now has targeted unit tests for:

- `FormatSelectorBuilder`
- `YoutubeRequestPlanner`
- `YoutubeAuthConfig`

---

## Active Risks And Technical Debt

### 1. AGP 9 still depends on temporary bridge flags

`gradle.properties` still carries:

- `android.builtInKotlin=false`
- `android.newDsl=false`
- `android.sourceset.disallowProvider=false`

These flags are deprecated and should be treated as a short-term bridge until the remaining legacy Android DSL and source-set usage is migrated.

### 2. Jetifier is still enabled

`android.enableJetifier=true` is also deprecated. The dependency graph should be reviewed so Jetifier can be removed before AGP 10 forces the issue.

### 3. CI still has packaging warnings

Recent build logs still show non-fatal warnings around:

- `android:extractNativeLibs` in `AndroidManifest.xml`
- stripping `libffmpeg.zip.so` and `libpython.zip.so`

These are not release blockers today, but they reduce signal quality in CI.

### 4. Hilt is compatible, but not yet fully modernized

The project now works on AGP 9 by using the explicit generated-base-class pattern instead of the old Hilt transform path. That fixes the immediate build failure, but the DI and annotation-processing setup is still kapt-based and worth revisiting later.

### 5. The YouTube BotGuard path is operationally fragile

The current PO-token flow depends on:

- BotGuard challenge endpoints
- shared constants mirrored from LibreTube
- WebView-readable YouTube configuration values

That is workable, but it is not a stable contract. The app should continue to treat PO-token generation as a best-effort recovery path and fail gracefully when upstream changes land.

### 6. Automated coverage is still narrow

There is no visible `androidTest` source tree in the current snapshot, and unit coverage does not yet touch several higher-risk areas:

- update-manager logic
- auth persistence boundaries
- media-tool validation and recovery
- runtime fallback behavior

---

## Recommended Next Steps

1. Remove the AGP 9 compatibility flags by migrating the remaining legacy Android DSL and source-set usage.
2. Audit dependencies so Jetifier can be disabled.
3. Clean the remaining manifest and native-library packaging warnings from CI.
4. Expand automated coverage around updates, auth persistence, and media-tool request validation.
5. Keep release docs aligned with the in-app-only YouTube access flow and the newer maintenance tooling.

---

## Conclusion

The project is healthier than the older audit suggested. The most important mismatch is gone: YouTube auth is now an in-app workflow instead of a desktop-helper workflow. The current concerns are mostly maintenance and migration concerns around AGP 9, deprecated Gradle flags, and limited automated coverage, not broken core product architecture.
