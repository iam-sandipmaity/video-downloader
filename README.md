# Video Downloader

Local-first Android video downloading powered by `yt-dlp` and `FFmpeg`.

Everything runs on-device:

- no backend
- no cloud conversion
- no forced account system
- no server-side link handling

[![Build](https://img.shields.io/github/actions/workflow/status/iam-sandipmaity/video-downloader/android-build.yml?label=build&logo=github)](https://github.com/iam-sandipmaity/video-downloader/actions/workflows/android-build.yml)
[![Platform](https://img.shields.io/badge/platform-Android%208%2B-3DDC84?logo=android&logoColor=white)](COMPATIBILITY.md)
[![Architecture](https://img.shields.io/badge/arch-arm64--v8a-blue)](COMPATIBILITY.md)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

## Project Status

The current `1.7.2` line is the app's stable UI baseline.

That means:

- the current navigation and main screen structure are expected to stay stable
- near-term updates are more likely to focus on bug fixes, download/runtime compatibility, translation quality, and internal logic hardening
- major UI overhauls are not the default short-term direction anymore

## App Preview

The screenshots below reflect the current app flow, and can be refreshed over
time as the UI evolves.

<p align="center">
  <img src="public/demo/home-page.png" alt="Home screen with URL input" width="220" />
  <img src="public/demo/home-link-analyzing.png" alt="Home screen while analyzing a link" width="220" />
  <img src="public/demo/single-file-download-option-view.png" alt="Single file download options overlay" width="220" />
</p>

<p align="center">
  <img src="public/demo/playlist-download.png" alt="Playlist download selection" width="200" />
  <img src="public/demo/playlist-download-time-any-randowm-file-formate-and-name-editing.png" alt="Playlist per-file format and rename controls" width="200" />
  <img src="public/demo/downloading-screen.png" alt="Queue screen while a task is active" width="200" />
  <img src="public/demo/downloading-tab.png" alt="Download queue tab" width="200" />
</p>

<p align="center">
  <img src="public/demo/download-tab.png" alt="Downloads library" width="200" />
  <img src="public/demo/downloaded-file-viewer-tab.png" alt="Saved file viewer tab" width="200" />
  <img src="public/demo/downloaded-audio-inapp-player.png" alt="In-app audio player" width="200" />
  <img src="public/demo/in-app-video-player.png" alt="In-app video player" width="200" />
</p>

<p align="center">
  <img src="public/demo/more-page.png" alt="More page" width="200" />
  <img src="public/demo/settings-page.png" alt="Settings page" width="200" />
  <img src="public/demo/appearence-page.png" alt="Appearance page" width="200" />
  <img src="public/demo/about-page-credit-section.png" alt="About page with credits section" width="200" />
</p>

<p align="center">
  <img src="public/demo/cookies-page.png" alt="Cookies page" width="200" />
  <img src="public/demo/youtube-po-generation-page.png" alt="YouTube access and PO generation page" width="200" />
  <img src="public/demo/converter.png" alt="Converter tool page" width="200" />
  <img src="public/demo/compressor.png" alt="Compressor tool page" width="200" />
</p>

<p align="center">
  <img src="public/demo/appearence-page-in-bengali.png" alt="Appearance page in Bengali" width="200" />
  <img src="public/demo/notification-page-in-bengali.png" alt="Notification settings in Bengali" width="200" />
</p>

## Key Features

- Video downloads with format, container, and quality selection
- Audio-only downloads including common music-friendly output formats
- Playlist downloads with global defaults and per-file overrides
- Download queue with pause, resume, retry, and diagnostics
- Saved downloads library with share, delete, and batch actions
- History with per-task logs
- Cookies and YouTube access recovery tools
- Built-in converter and compressor tools
- Updates center for app, `yt-dlp`, and `FFmpeg`
- Multi-language app UI with growing locale coverage

## Current Focus

The repository is currently optimized around:

- stable download behavior
- local runtime maintenance
- queue and history reliability
- translation coverage
- practical documentation

If a future update lands soon, it is more likely to be because of:

- site compatibility changes
- runtime update safety
- queue or download bugs
- translation cleanup
- internal logic/test improvements

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

## Compatibility

- Android 8.0 and above
- Primary shipped ABI: `arm64-v8a`
- Public downloads root: `Download/LocalDownloader/`

For custom ABI builds or deeper runtime details, see
[COMPATIBILITY.md](COMPATIBILITY.md).

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
- Gradle available in `PATH`

```bash
git clone https://github.com/iam-sandipmaity/video-downloader
cd video-downloader
gradle :app:assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Repository Docs

- [CHANGELOG.md](CHANGELOG.md)
- [COMPATIBILITY.md](COMPATIBILITY.md)
- [SECURITY.md](SECURITY.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [PROJECT_AUDIT.md](PROJECT_AUDIT.md)
- [future-plan.md](future-plan.md)

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
