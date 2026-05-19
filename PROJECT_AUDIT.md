# Project Audit Report: Video Downloader (Android)

## Audit Scope

- Audit date: 2026-05-20
- Current app version in `gradle.properties`: `1.7.0.1`
- Focus: repo state after `1.7.0`, build and CI migration, in-app YouTube access, queue recovery UX, and current maintenance risks

---

## Current Snapshot

Video Downloader is now a local-first Android app built around:

- Jetpack Compose UI
- Hilt-based dependency injection
- KSP-based annotation processing
- Room persistence plus DataStore-backed settings
- `youtubedl-android` runtime plus FFmpeg integration
- in-app YouTube cookie capture and PO-token generation
- app / `yt-dlp` / FFmpeg update management from a dedicated Updates flow
- first-run setup guidance plus in-queue troubleshooting shortcuts

The biggest architectural correction since the older audit is that the project no longer depends on a desktop auth handoff. The removed `tools/youtube-auth-helper` path has been replaced by an Android-native flow using `YoutubeAuthScreen`, `WebViewCookieExporter`, and `YoutubePoTokenGenerator`.

---

## Post-1.7.0 Change Summary

The repo picked up 6 maintenance commits after `1.7.0`, then a feature branch focused on download UX and AGP cleanup:

1. Added security policy and maintenance docs
2. Bumped the app to the `1.7.0.1` line
3. Applied Dependabot-driven Android, Gradle, and GitHub Actions upgrades
4. Raised `compileSdk` to `36` for the newer AndroidX dependency set
5. Removed the old Hilt Gradle plugin transform dependency for AGP 9 compatibility
6. Removed the unused desktop YouTube auth helper and its npm / Playwright maintenance surface

Current branch-level upgrades on top of that:

1. Added a two-step onboarding sheet for first-run cookie and YouTube access guidance
2. Added queue-level recovery shortcuts and in-app log export / issue-report actions
3. Refined queue cards with source branding and clearer retry affordances
4. Added estimated format size fallback when exact yt-dlp size metadata is missing
5. Migrated annotation processing from kapt to KSP and removed AGP 9 bridge flags

This is now a mixed maintenance and product-polish line rather than a pure maintenance-only patch line.

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

### 5. Download troubleshooting is much more actionable

- first-run users now get a cleaner setup path instead of a passive reminder card
- failed or stuck queue items now surface cookies, PO generation, log export, and issue reporting where users actually need them
- queue cards identify known source sites visually, which makes mixed download lists easier to scan

### 5. The repo is no longer test-free

There is still only light coverage, but the repo now has targeted unit tests for:

- `FormatSelectorBuilder`
- `YoutubeRequestPlanner`
- `YoutubeAuthConfig`

---

## Active Risks And Technical Debt

### 1. CI still has packaging warnings

Recent build logs still show non-fatal warnings around:

- stripping `libffmpeg.zip.so` and `libpython.zip.so`

These are not release blockers today, but they reduce signal quality in CI.

### 2. Build verification is environment-blocked locally

The AGP cleanup is in better shape now, but this machine still has no Android SDK path configured. That means local verification currently stops at SDK detection instead of running the full Android compile.

### 3. The YouTube BotGuard path is operationally fragile

The current PO-token flow depends on:

- BotGuard challenge endpoints
- shared constants mirrored from LibreTube
- WebView-readable YouTube configuration values

That is workable, but it is not a stable contract. The app should continue to treat PO-token generation as a best-effort recovery path and fail gracefully when upstream changes land.

### 4. Automated coverage is still narrow

There is no visible `androidTest` source tree in the current snapshot, and unit coverage does not yet touch several higher-risk areas:

- update-manager logic
- auth persistence boundaries
- onboarding visibility rules
- queue recovery affordances
- media-tool validation and recovery
- runtime fallback behavior

---

## Recommended Next Steps

1. Validate the post-kapt, KSP-based AGP 9 build on a fully configured Android SDK machine.
2. Clean the remaining native-library packaging warnings from CI.
3. Expand automated coverage around updates, onboarding visibility, queue recovery, and media-tool request validation.
4. Keep release docs aligned with the onboarding sheet, queue recovery UX, and in-app-only YouTube access flow.

---

## Conclusion

The project is healthier than the older audit suggested. The biggest architectural mismatch is gone: YouTube auth is now an in-app workflow instead of a desktop-helper workflow, and the AGP 9 migration no longer relies on the older bridge flags. The remaining concerns are mostly CI hygiene, environment-specific verification, and limited automated coverage rather than broken core product architecture.
