<p align="center">
  <img src="public/logo.svg" alt="Video Downloader logo" width="80" height="80" />
</p>

<h1 align="center">Video Downloader</h1>

<p align="center">
  Local-first Android video downloading powered by <code>yt-dlp</code> and <code>FFmpeg</code>.
</p>

<p align="center">
  Everything runs on-device: no backend, no cloud conversion, no forced account system, and no server-side link handling.
</p>

<p align="center">
  <a href="https://github.com/iam-sandipmaity/video-downloader/actions/workflows/master.yml">
    <img alt="CI" src="https://img.shields.io/github/actions/workflow/status/iam-sandipmaity/video-downloader/master.yml?label=ci&logo=github" />
  </a>
  <a href="https://github.com/iam-sandipmaity/video-downloader/releases">
    <img alt="Release" src="https://img.shields.io/github/v/release/iam-sandipmaity/video-downloader?include_prereleases&label=release" />
  </a>
  <a href="https://github.com/iam-sandipmaity/video-downloader/releases">
    <img alt="Downloads" src="https://img.shields.io/github/downloads/iam-sandipmaity/video-downloader/total?label=downloads" />
  </a>
  <a href="https://hosted.weblate.org/projects/local-video-downloader/android-app-strings/">
    <img alt="Translate on Hosted Weblate" src="https://img.shields.io/badge/translate-Hosted%20Weblate-2dbcae?logo=weblate&logoColor=white" />
  </a>
  <a href="https://video.sandipmaity.me">
    <img alt="Website" src="https://img.shields.io/badge/website-video.sandipmaity.me-orange" />
  </a>
  <a href="COMPATIBILITY.md">
    <img alt="Platform" src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84?logo=android&logoColor=white" />
  </a>
  <a href="COMPATIBILITY.md">
    <img alt="Architecture" src="https://img.shields.io/badge/arch-arm64--v8a-blue" />
  </a>
  <a href="LICENSE">
    <img alt="License" src="https://img.shields.io/badge/license-MIT-green" />
  </a>
</p>

## Why This App

| Local-first | Real downloader stack | Built for recovery | Useful beyond downloads |
| --- | --- | --- | --- |
| Analyze links, download media, merge streams, and convert files directly on the device. | Uses embedded `yt-dlp` plus managed `FFmpeg` paths instead of a remote relay service. | Queueing, retries, diagnostics, cookies, YouTube access help, and runtime updates are part of the product flow. | Includes saved-library browsing, audio/video playback, compression, conversion, and history tools. |

## Download

### Stable Release

| Obtainium | GitHub |
| --- | --- |
| <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/iam-sandipmaity/video-downloader"><img src="https://github.com/ImranR98/Obtainium/blob/main/assets/graphics/badge_obtainium.png?raw=true" alt="Get it on Obtainium" width="250" /></a> | <a href="https://github.com/iam-sandipmaity/video-downloader/releases"><img src="https://raw.githubusercontent.com/rubenpgrady/get-it-on-github/refs/heads/main/get-it-on-github.png" alt="Get it on GitHub" width="250" /></a> |

### Nightly / Debug Build

> Nightly builds are unstable and may contain bugs. Use at your own risk.

<p align="center">
  <a href="https://nightly.link/iam-sandipmaity/video-downloader/workflows/master.yml/main/nightly-debug-apk">
    <img src="https://raw.githubusercontent.com/rubenpgrady/get-it-on-github/refs/heads/main/get-it-on-github.png" alt="Get latest nightly debug build" width="250" />
  </a>
</p>

<p align="center">
  <strong>Latest nightly debug build artifact</strong> - always points to the newest successful <code>main</code> build
</p>

### Requirements

- Minimum requirement: Android 8.0+

## What You Can Do

- Paste a supported link, analyze it locally, and choose from the formats `yt-dlp` exposes for that source.
- Download single videos, audio-only files, or playlists with global defaults plus per-item overrides.
- Rename outputs before download, keep source thumbnails, and let the app handle post-processing when audio/video streams need merging.
- Pause, resume, retry, inspect, and recover queue items without needing a desktop companion app.
- Open completed downloads in the built-in audio or video players, or manage them from the local library with share and delete actions.
- Use cookies and YouTube access helpers when a site needs session data for better compatibility.
- Run conversion and compression workflows directly on the device after a download finishes.

## How The App Feels In Practice

| Before download | While downloading | After download |
| --- | --- | --- |
| Link analysis, format selection, naming control, cookies, and access recovery are built into the main flow. | Queue diagnostics, pause/resume behavior, and worker-backed retries help tougher downloads stay manageable. | Saved media stays available in the local library with playback, sharing, cleanup, and follow-up media tools. |

## Practical Focus

- Stability-first download behavior instead of backend-heavy automation.
- Local processing with minimal trust in external services.
- Recovery paths for difficult sites, network hiccups, and post-processing failures.
- UI surfaces that stay useful for regular downloading instead of exposing raw runtime complexity everywhere.

For release-specific changes, see [CHANGELOG.md](CHANGELOG.md).

## App Preview

### Home And Link Analysis

| Home | Link Analyzing | Single Download Sheet |
| --- | --- | --- |
| <img src="public/demo/home-page.png" alt="Home screen with URL input" width="200" /> | <img src="public/demo/home-link-analyzing.png" alt="Home screen while analyzing a link" width="200" /> | <img src="public/demo/single-file-download-option-view.png" alt="Single file download options overlay" width="200" /> |
| URL entry, ready-history cards, and the main download starting point. | Active metadata extraction before formats are shown. | Format, naming, and final output controls for a single media item. |

### Playlist And Queue

| Playlist Picker | Per-File Playlist Controls |
| --- | --- |
| <img src="public/demo/playlist-download.png" alt="Playlist download selection" width="200" /> | <img src="public/demo/playlist-download-time-any-randowm-file-formate-and-name-editing.png" alt="Playlist per-file format and rename controls" width="200" /> |
| Global playlist selection before queuing. | Item-level overrides for format and file naming. |

| Active Queue Screen | Queue Tab |
| --- | --- |
| <img src="public/demo/downloading-screen.png" alt="Queue screen while a task is active" width="200" /> | <img src="public/demo/downloading-tab.png" alt="Download queue tab" width="200" /> |
| Live task progress with worker-driven updates. | Broader queue view for running, queued, and failed items. |

### Library And Playback

| Downloads Library | Saved File Viewer |
| --- | --- |
| <img src="public/demo/download-tab.png" alt="Downloads library" width="200" /> | <img src="public/demo/downloaded-file-viewer-tab.png" alt="Saved file viewer tab" width="200" /> |
| Local media library with file actions and browsing. | File-centric view of saved downloads. |

| In-App Audio Player | In-App Video Player |
| --- | --- |
| <img src="public/demo/downloaded-audio-inapp-player.png" alt="In-app audio player" width="200" /> | <img src="public/demo/in-app-video-player.png" alt="In-app video player" width="200" /> |
| Music-style playback for downloaded audio. | Full-screen player flow for downloaded video. |

### Settings And Support Surfaces

| More Page | Settings Hub |
| --- | --- |
| <img src="public/demo/more-page.png" alt="More page" width="200" /> | <img src="public/demo/settings-page.png" alt="Settings page" width="200" /> |
| Utility shortcuts, tools, updates, and help entry points. | Main settings navigation with grouped categories. |

| Appearance Page | About And Credits |
| --- | --- |
| <img src="public/demo/appearence-page.png" alt="Appearance page" width="200" /> | <img src="public/demo/about-page-credit-section.png" alt="About page with credits section" width="200" /> |
| Theme, accent, contrast, and language-adjacent presentation controls. | Credits and upstream acknowledgements inside the app. |

### Access And Media Tools

| Cookies Page | YouTube Access |
| --- | --- |
| <img src="public/demo/cookies-page.png" alt="Cookies page" width="200" /> | <img src="public/demo/youtube-po-generation-page.png" alt="YouTube access and PO generation page" width="200" /> |
| Saved cookie/session management for harder sites. | Dedicated YouTube session and access recovery flow. |

| Converter | Compressor |
| --- | --- |
| <img src="public/demo/converter.png" alt="Converter tool page" width="200" /> | <img src="public/demo/compressor.png" alt="Compressor tool page" width="200" /> |
| Format conversion utility built into the app. | Compression workflow for media size reduction. |

### Localization Examples

| Appearance In Bengali | Notification Settings In Bengali |
| --- | --- |
| <img src="public/demo/appearence-page-in-bengali.png" alt="Appearance page in Bengali" width="200" /> | <img src="public/demo/notification-page-in-bengali.png" alt="Notification settings in Bengali" width="200" /> |
| One of the localized settings surfaces. | Another example of translated in-app settings UI. |

## Feature Set

- Video downloads with format, quality, and container selection.
- Audio-only downloads with music-friendly outputs.
- Playlist downloads with global defaults and per-item overrides.
- Download queue with pause, resume, retry, and diagnostics.
- Saved downloads library with share, delete, and batch actions.
- Download history with per-task logs and traces.
- Cookies and YouTube access recovery tools.
- Built-in converter and compressor flows.
- Updates center for the app, `yt-dlp`, and `FFmpeg`.
- Multi-language UI with expanding coverage.

## Supported Languages

Current in-app language support includes:

- English
- Hindi
- Bengali
- Tamil
- Telugu
- Kannada
- Malayalam
- Korean
- Japanese
- Simplified Chinese

## Translation Progress

<p align="center">
  <a href="https://hosted.weblate.org/projects/local-video-downloader/android-app-strings/">
    <img src="https://img.shields.io/badge/translate-Hosted%20Weblate-2dbcae?logo=weblate&logoColor=white" alt="Translate on Hosted Weblate" />
  </a>
</p>

Want to improve a translation? Use
[Hosted Weblate](https://hosted.weblate.org/projects/local-video-downloader/android-app-strings/)
or send a direct pull request against the Android resource files. The project is
migrating translation work from Crowdin to Weblate so community translations can
continue on libre hosting.

## Compatibility

- Android 8.0 and above
- Primary shipped ABI: `arm64-v8a`
- Public downloads root: `Download/LocalDownloader/`

For custom ABI builds or deeper runtime details, see [COMPATIBILITY.md](COMPATIBILITY.md).

## Architecture Summary

The app is built around:

- Kotlin
- Jetpack Compose
- MVVM-style state handling
- Hilt dependency injection
- WorkManager background execution
- Room and DataStore persistence
- embedded `youtubedl-android` runtime
- packaged and managed `FFmpeg` runtime paths

High-level flow:

1. Paste a link and analyze it.
2. Select format, naming, and optional extras.
3. Queue the task through WorkManager.
4. Execute `yt-dlp` locally.
5. Apply `FFmpeg` post-processing when needed.
6. Save outputs into the public download folders and app library.

For deeper implementation notes, see:

- [docs/architecture.md](docs/architecture.md)
- [docs/development.md](docs/development.md)
- [IMPLEMENTATION.md](IMPLEMENTATION.md)

## Build From Source

Requirements:

- JDK 17
- Android SDK
- use the bundled Gradle wrapper from the repository root

```bash
git clone https://github.com/iam-sandipmaity/video-downloader
cd video-downloader
./gradlew :app:assembleStandardDebug
```

Use `gradlew.bat` instead of `./gradlew` when running from PowerShell or Command Prompt on Windows.

Debug APK output:

```text
app/build/outputs/apk/standard/debug/app-standard-debug.apk
```

Repo-safe build for IzzyOnDroid or F-Droid style distribution:

```bash
./gradlew :app:assembleRepoSafeDebug
```

Repo-safe APK output:

```text
app/build/outputs/apk/repoSafe/debug/app-repoSafe-debug.apk
```

## Repository Docs

- [CHANGELOG.md](CHANGELOG.md)
- [COMPATIBILITY.md](COMPATIBILITY.md)
- [PRIVACY.md](PRIVACY.md)
- [KEYSTORE_SETUP.md](KEYSTORE_SETUP.md)
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md)
- [SECURITY.md](SECURITY.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [PROJECT_AUDIT.md](PROJECT_AUDIT.md)
- [future-plan.md](future-plan.md)

## Donate

If this project helps you, you can support ongoing development on [Razorpay](https://razorpay.me/@maitysandip).

## License

This project is licensed under the [MIT License](LICENSE).

## Credits

This app builds on top of multiple open-source tools and libraries, including:

- `yt-dlp`
- `FFmpeg`
- Android Jetpack
- Kotlin
- Material 3

The in-app About section also lists upstream credits and linked sources.

## Star History

<a href="https://www.star-history.com/#iam-sandipmaity/video-downloader&Timeline">
 <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=iam-sandipmaity/video-downloader&type=timeline&legend=top-left" />
</a>

## Contributors

<p align="center">
  <a href="https://github.com/iam-sandipmaity/video-downloader/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=iam-sandipmaity/video-downloader" width="85%"/>
  </a>
</p>
