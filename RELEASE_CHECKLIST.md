# Release Checklist

Use this checklist before publishing a new app release or promoting Nightly changes to Stable.
For full details on the branching workflow, see [docs/BRANCHING_STRATEGY.md](docs/BRANCHING_STRATEGY.md).

---

## 🌙 Nightly Pre-release Checklist

Nightly builds are automatically generated and published on every merge to `nightly` (and via daily 03:00 UTC cron).

- [ ] Confirm changes are merged into the `nightly` branch
- [ ] If bumping nightly version, update `NIGHTLY_VERSION_CODE` and `NIGHTLY_VERSION_NAME` in `gradle.properties`
- [ ] Document new features and bugfixes in `CHANGELOG-NIGHTLY.md`
- [ ] Verify GitHub Actions `release-nightly` completes successfully and updates the `nightly` release tag

---

## 🚀 Stable Release Checklist (Promoting Nightly to Main)

### 1. Metadata & Versioning
- [ ] Merge tested changes from `nightly` into a release PR targeting `main`
- [ ] Increment `APP_VERSION_CODE` in `gradle.properties` (must be strictly greater than previous release)
- [ ] Set `APP_VERSION_NAME` to the new stable version (e.g. `1.7.6.0`)
- [ ] Move/summarize recent entries from `CHANGELOG-NIGHTLY.md` into `CHANGELOG.md` under `## [X.Y.Z] - YYYY-MM-DD`

### 2. Build & Automated Tests
- [ ] Run `./gradlew :app:assembleStandardDebug`
- [ ] Run `./gradlew :app:testStandardDebugUnitTest`
- [ ] Confirm no minification/ProGuard issues with `./gradlew :app:assembleStandardRelease`
- [ ] Confirm update flows still gate correctly while downloads are active
- [ ] Confirm no new runtime-selection regressions in `yt-dlp` or FFmpeg paths

### 3. UI & Core Feature Verification
- [ ] Verify Home / Browse analysis and download bottom sheet
- [ ] Verify single-file and playlist download flows
- [ ] Verify queue, history, and vault screens
- [ ] Verify subtitle selection and in-player rendering
- [ ] Verify converter and compressor basic flows

### 4. Localization & Documentation
- [ ] Check supported languages for fallback-to-English regressions
- [ ] Check long titles, buttons, and empty states for clipping
- [ ] Confirm README screenshots and compatibility notes are still accurate

### 5. Release Publication
- [ ] Merge the release PR into `main`
- [ ] GitHub Actions `release-main` will automatically build the signed release APK and create the GitHub release
- [ ] Sync `main` back into `nightly` (`git checkout nightly && git merge main && git push origin nightly`)
