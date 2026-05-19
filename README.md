<div align="center">

<svg width="80" height="80" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="32" height="32" rx="6" fill="url(#g)"/>
  <g transform="translate(8,8)">
    <path d="M8 2L8 10" stroke="white" stroke-width="2" stroke-linecap="round"/>
    <path d="M8 10L5 7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M8 10L11 7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M3 11L3 13Q3 14,4 14L12 14Q13 14,13 13L13 11" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
  </g>
  <defs>
    <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#14B8A6"/>
      <stop offset="100%" style="stop-color:#10B981"/>
    </linearGradient>
  </defs>
</svg>

# Video Downloader

**A fully local, privacy-first Android video downloader powered by `yt-dlp` + `FFmpeg`.**  
No server. No cloud. No tracking. Everything runs on your device.

[![Build](https://img.shields.io/github/actions/workflow/status/iam-sandipmaity/video-downloader/android-build.yml?label=build&logo=github)](https://github.com/iam-sandipmaity/video-downloader/actions/workflows/android-build.yml)
[![Platform](https://img.shields.io/badge/platform-Android%208%2B-3DDC84?logo=android&logoColor=white)](COMPATIBILITY.md)
[![Architecture](https://img.shields.io/badge/arch-arm64--v8a-blue)](#binary-integration)
[![License](https://img.shields.io/badge/license-MIT-green)](#)

</div>

---

## ✨ Features

| Feature | Details |
|---|---|
| 🎬 **Video download** | mp4, webm, mkv, mov |
| 🎵 **Audio-only** | mp3, m4a, aac, wav, opus, flac |
| 📋 **Format picker** | Quality selector (144p → 4K), stream type, container |
| 📥 **Download queue** | Real-time progress, speed, size info, pause/resume/cancel |
| 📜 **History** | Completed and failed downloads with timestamps, saved paths, and log access |
| 🔄 **Updates center** | Check and install app, `yt-dlp`, and `FFmpeg` updates from inside the app |
| 🌐 **1000+ sites** | YouTube, Instagram, TikTok, X/Twitter, Reddit, Vimeo, SoundCloud, and more |
| 🔒 **100% local** | No backend, no accounts, no cloud |
| 📁 **Public Downloads** | Files saved to `/sdcard/Download/LocalDownloader/` |
| ⚙️ **Settings** | Persistent defaults for folders, filename templates, subtitles, and media output |

---

## 📱 Compatibility

- **Android 8.0 (Oreo) and above** — API 26+
- **ARM64 devices** — virtually all phones sold since 2015
- Covers ~90% of all active Android devices worldwide

> For x86, armeabi-v7a or other architectures → see **[COMPATIBILITY.md](COMPATIBILITY.md)**

---

## 🏗️ Architecture

```
URL Input
   │
   ▼
Compose UI  ──→  ViewModel  ──→  Repository
                                    │
                          ┌─────────┴──────────┐
                          ▼                    ▼
                    WorkManager           DataStore
                          │             (settings)
                          ▼
                   DownloadEngine
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
       embedded yt-dlp          ffmpeg binary
           runtime               (local asset)
              │
              ▼
     /sdcard/Download/LocalDownloader/
```

**Stack:** Kotlin · Jetpack Compose · MVVM + Clean Architecture · Hilt DI · WorkManager

---

## 📂 Project structure

```
app/src/main/
├── java/com/localdownloader/
│   ├── ui/
│   │   ├── screens/          # Browser, Downloads, History, Settings, Updates, player flows
│   │   ├── components/       # UrlInput, VideoCard, DownloadProgress, FormatSelector
│   │   └── theme/
│   ├── viewmodel/            # DownloadViewModel, FormatViewModel, UpdatesViewModel
│   ├── domain/
│   │   ├── models/           # DownloadTask, VideoInfo, DownloadOptions …
│   │   ├── repositories/     # DownloaderRepository interface
│   ├── data/                 # DownloadRepositoryImpl, Room persistence, SettingsStore
│   ├── downloader/           # YtDlpExecutor, DownloadEngine, ProgressParser …
│   ├── updates/              # GitHub release client + app/runtime update managers
│   ├── worker/               # DownloadWorker + yt-dlp update workers
│   ├── utils/                # FileUtils, Logger, UrlValidator
│   └── di/                   # Hilt modules
├── assets/
│   ├── ffmpeg/arm64-v8a/ffmpeg     ← fallback executable
│   └── changelog/CHANGELOG.md      ← bundled release notes shown in-app
└── jniLibs/
    └── arm64-v8a/
        └── libffmpeg_exec.so        ← bundled native fallback
```

---

## ⚙️ Binary integration

The app uses the embedded `youtubedl-android` runtime for `yt-dlp`, and can replace that runtime through the in-app Updates screen.

`ffmpeg` now resolves in this order:

1. **Managed overlay package** — app-owned runtime downloaded from the Updates screen
2. **Embedded runtime package** — packaged `libffmpeg.so` with support files when available
3. **Bundled native fallback** — `libffmpeg_exec.so` shipped with the app
4. **Copied executable fallback** — raw asset copied to internal storage if native launch fails

Default shipped ABIs:

| File | Location |
|---|---|
| `libffmpeg_exec.so` | `jniLibs/arm64-v8a/` |
| `ffmpeg` (fallback) | `assets/ffmpeg/arm64-v8a/` |

Managed overlay packages and the embedded FFmpeg runtime dependency may also provide `libffmpeg.so` and `libffmpeg.zip.so` at runtime.

> Need a different architecture? See [COMPATIBILITY.md](COMPATIBILITY.md) for step-by-step instructions.

---

## 🚀 Build from source

**Requirements:** JDK 17 · Android SDK · Gradle 8.7

```bash
git clone https://github.com/your-username/video-downloader
cd video-downloader
gradle :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

**Or use GitHub Actions** — every push auto-builds a debug APK available under the **Actions** tab → latest workflow run → `app-debug-apk` artifact.  
Tagged releases (`v*`) also produce a release APK attached to the GitHub Release.

## Stable update installs

Android only treats a new APK as an update when all of these stay aligned:

- same package name
- higher `versionCode`
- same signing certificate

This project now supports persistent signing for both install channels:

- internal debug APK: `com.localdownloader.debug`
- production release APK: `com.localdownloader`

To configure local signing:

1. Copy `keystore.properties.example` to `keystore.properties`
2. Fill in the stable debug and/or release keystore values
3. Keep the real keystore files and `keystore.properties` out of git

CI expects these secrets for stable update-compatible artifacts:

- `INTERNAL_DEBUG_KEYSTORE_BASE64`
- `INTERNAL_DEBUG_STORE_PASSWORD`
- `INTERNAL_DEBUG_KEY_ALIAS`
- `INTERNAL_DEBUG_KEY_PASSWORD`
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Important:

- If a user already installed an APK signed by a different key, Android cannot upgrade it in place.
- That old install must be uninstalled once.
- After switching to the stable keystore, future APKs from that same channel will install as updates.

## In-app updates

The app now includes an Updates screen for three separate flows:

- app release checks and APK install handoff from GitHub releases
- `yt-dlp` channel selection (`stable`, `nightly`, `master`) plus manual or startup-triggered updates
- `FFmpeg` managed runtime installation layered on top of the bundled fallback binaries

Runtime updates are blocked while downloads are actively running so file replacement stays safe.

---

## 🌐 Supported sites

Any site in the [yt-dlp supported sites list](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md) — including:

YouTube · Instagram · TikTok · X / Twitter · Reddit · Facebook · Vimeo · SoundCloud · Dailymotion · Twitch · Pinterest · and 1000+ more

---

<div align="center">

Made with ❤️ · runs entirely on your phone · no data ever leaves your device

</div>

## Build

1. Install Android SDK + JDK 17.
2. Add binaries in `app/src/main/assets/...`.
3. Build:

```bash
gradle :app:assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Open-source compliance notes

- Ensure your use of `yt-dlp` and downloaded content follows local law and platform terms.
- Keep license and attribution files for any bundled binaries.

## More documentation

- [Architecture docs](docs/architecture.md)
- [Development docs](docs/development.md)

## Runtime logs

The app now writes persistent logs to internal storage:

- `/data/user/0/<applicationId>/files/logs/app.log`
- rotated backup: `/data/user/0/<applicationId>/files/logs/app.log.1`
- `/data/user/0/<applicationId>/files/logs/crash.log` (warnings/errors + stack traces)
- mirrored external file (if available): `/storage/emulated/0/Android/data/<applicationId>/files/logs/app.log`
- mirrored external crash file (if available): `/storage/emulated/0/Android/data/<applicationId>/files/logs/crash.log`

Logs include activity lifecycle, analyze flow, yt-dlp command execution, worker progress, and failures.
