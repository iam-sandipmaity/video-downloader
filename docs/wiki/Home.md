# Video Downloader Wiki

<p align="center">
  <img src="https://raw.githubusercontent.com/iam-sandipmaity/video-downloader/main/public/logo.svg" alt="Video Downloader logo" width="96" height="96" />
</p>

<p align="center">
  Local-first Android media downloading powered by <code>yt-dlp</code> and
  <code>FFmpeg</code>.
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/iam-sandipmaity/video-downloader/main/public/demo/home-page.png" alt="Video Downloader home screen" width="260" />
</p>

## What This App Is

Video Downloader is an Android app that analyzes links, downloads media,
manages queues, merges streams, and runs conversion or compression directly on
your device.

Core ideas:

- local-first execution instead of a project-owned backend
- built around `yt-dlp` for extractor coverage and `FFmpeg` for media work
- useful recovery tools for difficult sites and difficult downloads
- a real local library with built-in audio and video playback

## What You Can Do

- Analyze a supported link and choose from available formats.
- Download video, audio-only, or playlists.
- Rename outputs before download.
- Use cookies or YouTube access helpers when a site needs session data.
- Pause, resume, retry, inspect, and recover downloads from the queue.
- Open completed media in the built-in players.
- Convert or compress local files after download.
- Update app runtimes like `yt-dlp` and `FFmpeg` from inside the app.

## Download Options

- Stable APKs:
  `https://github.com/iam-sandipmaity/video-downloader/releases`
- Obtainium source:
  `https://github.com/iam-sandipmaity/video-downloader`
- Nightly builds:
  `https://nightly.link/iam-sandipmaity/video-downloader/workflows/android-build.yml/main`

Current device baseline:

- Android 8.0+
- primary shipped ABI: `arm64-v8a`

## Start Here

- [Getting Started](Getting-Started.md)
- [Download Workflow](Download-Workflow.md)
- [Library, Playback, and Media Tools](Library-Playback-and-Media-Tools.md)
- [Settings, Cookies, and YouTube Access](Settings-Cookies-and-YouTube-Access.md)
- [Troubleshooting](Troubleshooting.md)
- [FAQ](FAQ.md)

## Good Pages For Maintainers

- [Build and Development](Build-and-Development.md)
- [Architecture and Code Map](Architecture-and-Code-Map.md)
- [Updates, Runtimes, and Compatibility](Updates-Runtimes-and-Compatibility.md)
- [Privacy, Security, and Responsible Use](Privacy-Security-and-Responsible-Use.md)

## Quick Start Summary

1. Install the app on an Android 8.0+ device.
2. Paste a media link into the Home screen.
3. Wait for local analysis to finish.
4. Pick the format, naming, and optional download settings you want.
5. Start the task and manage progress from the queue.
6. Open the saved result from the Downloads library.

## Key Concepts

### Local-First

The app is designed to do the work on-device:

- link analysis
- downloads
- stream merging
- conversion
- compression

### Runtime Updates Matter

Extractor compatibility changes often. The built-in Updates flow exists because
many site issues are fixed by newer `yt-dlp` or FFmpeg runtime behavior rather
than by a full app reinstall.

### Recovery Is Part Of The Product

This app is not only a "start download and hope" wrapper. The queue, history,
cookies, runtime updates, and diagnostics are all part of the intended
workflow.

## Useful Repository Docs

- Changelog:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/CHANGELOG.md`
- Compatibility:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/COMPATIBILITY.md`
- Privacy:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/PRIVACY.md`
- Security:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/SECURITY.md`
- Contributing:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/CONTRIBUTING.md`

## Need More Detail?

- For a visual tour:
  [Screenshots and Feature Tour](Screenshots-and-Feature-Tour.md)
- For common problems:
  [Troubleshooting](Troubleshooting.md)
- For source-level work:
  [Build and Development](Build-and-Development.md)
