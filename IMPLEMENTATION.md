# Current Implementation Notes

## Version Scope

- Baseline feature release: `1.7.0`
- Current maintenance line: `1.7.0.1`
- Current feature branch work: onboarding, queue troubleshooting polish, smarter format estimates, and AGP 9 cleanup on top of `1.7.0.1`

---

## 1. In-App YouTube Access Implementation

The project no longer uses the removed desktop `youtube-auth-helper` flow.

Current implementation:

1. The user opens the in-app YouTube access screen.
2. A dedicated WebView loads `YoutubePoTokenGenerator.SAMPLE_VIDEO_URL`.
3. The app reads `VISITOR_DATA` and `DATASYNC_ID` from the active YouTube page.
4. `WebViewCookieExporter` exports the matching YouTube and Google cookies from the app WebView session.
5. `YoutubePoTokenGenerator` runs the BotGuard flow locally and produces:
   - GVS token
   - player token
   - subtitle token
6. `FormatViewModel` stores the generated `YoutubeAuthConfig` together with the exported cookie text so later download requests can reuse it.

Important points:

- auth data is generated on-device
- the login session, cookies, and PO tokens come from the same WebView flow
- the Android app does not depend on Node.js, Playwright, or any desktop handoff
- current BotGuard constants are documented as mirrored from LibreTube for future maintenance tracking

Relevant files:

- `app/src/main/java/com/localdownloader/ui/screens/YoutubeAuthScreen.kt`
- `app/src/main/java/com/localdownloader/utils/WebViewCookieExporter.kt`
- `app/src/main/java/com/localdownloader/utils/YoutubePoTokenGenerator.kt`
- `app/src/main/java/com/localdownloader/viewmodel/FormatViewModel.kt`

---

## 2. Update System Implementation

The app now has a dedicated update subsystem for:

- app APK updates
- `yt-dlp` runtime updates
- FFmpeg runtime updates

Main pieces:

- `updates/GitHubReleaseClient.kt`
  Fetches GitHub release metadata and downloads assets.
- `updates/AppUpdateManager.kt`
  Chooses the best APK asset for the device ABI and prepares installs.
- `updates/YtDlpUpdateManager.kt`
  Installs `yt-dlp` runtime updates from the selected channel.
- `updates/FfmpegUpdateManager.kt`
  Handles FFmpeg overlay and runtime replacement.
- `viewmodel/UpdatesViewModel.kt`
  Keeps Updates screen state and user actions together.
- `worker/YtDlpUpdateScheduler.kt` and `worker/YtDlpUpdateWorker.kt`
  Run background `yt-dlp` maintenance with retry and defer handling.

The project `CHANGELOG.md` is copied into app assets through `syncBundledChangelog`, which lets the app show release notes in the Updates flow.

---

## 3. Build And CI Implementation

Current build state:

- AGP `9.2.1`
- Kotlin `2.3.21`
- Compose Gradle plugin enabled
- KSP-based annotation processing for Hilt, Hilt Work, and Room
- `compileSdk = 36`
- `targetSdk = 35`
- Java and Kotlin target = `17`

Recent compatibility work:

- GitHub Actions updated to newer action major versions
- CI Gradle version moved to `9.4.1`
- `compileSdk` raised to `36` for the newer AndroidX stack
- Hilt Gradle plugin transform dependency removed
- `DownloaderApplication`, `MainActivity`, and `AudioPlaybackService` now use the explicit generated-base-class pattern for Hilt compatibility on AGP 9
- temporary AGP bridge flags removed after switching the project off `kapt`
- generated changelog asset wiring moved off the older deprecated source-set call path

Relevant files:

- `build.gradle.kts`
- `app/build.gradle.kts`
- `.github/workflows/android-build.yml`
- `.github/workflows/cleanup.yml`
- `gradle.properties`

---

## 4. Security And Maintenance Surface

Repository-level maintenance additions now include:

- `SECURITY.md` for vulnerability reporting guidance
- `.github/dependabot.yml` for Gradle and GitHub Actions update monitoring
- code comments and development notes that document where the current BotGuard constants came from

The old npm and Playwright helper was removed, so the repo no longer needs an npm maintenance track just to support YouTube auth.

---

## 5. Temporary Compatibility Layer

The earlier AGP 9 migration bridge flags have now been removed:

- `android.builtInKotlin=false`
- `android.newDsl=false`
- `android.sourceset.disallowProvider=false`
- `android.enableJetifier=true`

What remains now is normal follow-up cleanup rather than a forced bridge layer:

- validate the KSP-based AGP 9 build on a machine with a configured Android SDK
- clean the remaining native `*.zip.so` strip warnings in CI
- keep Hilt / Room processor versions aligned with future Kotlin and AGP updates

---

## 6. Known Follow-Up Work

1. Expand automated coverage around update flows, auth persistence, playlist queue handling, and media-tool validation.
2. Clean CI warnings related to native `*.zip.so` stripping.
3. Validate the post-kapt AGP 9 setup on a fully configured Android SDK environment.
4. Keep release docs aligned with the in-app onboarding, queue recovery, and in-app-only YouTube access implementation.
