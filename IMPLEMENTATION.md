# Current Implementation Notes

## Version Scope

- current stable UI baseline: `1.7.2`
- current engineering emphasis: reliability, runtime maintenance, translation
  quality, and internal logic hardening

This document is meant to describe what the app is doing now, not an older
mid-transition snapshot.

---

## 1. Download Flow

The primary download path is:

1. User pastes or shares a URL into Home / Browse.
2. `FormatViewModel` requests analysis.
3. `FormatExtractor` runs `yt-dlp -J ...` locally.
4. The app builds a format-selection sheet from the returned media data.
5. The user confirms output format, naming, playlist behavior, and optional
   metadata/subtitle settings.
6. The repository schedules work through WorkManager.
7. `DownloadWorker` calls `DownloadEngine`.
8. `DownloadEngine` invokes `yt-dlp` locally and coordinates FFmpeg when
   post-processing is needed.
9. The queue, history, and downloads surfaces are updated from task state and
   saved output data.

Relevant files:

- `app/src/main/java/com/localdownloader/viewmodel/FormatViewModel.kt`
- `app/src/main/java/com/localdownloader/downloader/FormatExtractor.kt`
- `app/src/main/java/com/localdownloader/downloader/DownloadEngine.kt`
- `app/src/main/java/com/localdownloader/worker/DownloadWorker.kt`

---

## 2. Runtime Resolution

### yt-dlp

The app uses the embedded `youtubedl-android` runtime and can replace that
runtime through the Updates flow.

### FFmpeg

FFmpeg resolution now follows a layered path:

1. managed overlay package downloaded by the app
2. embedded runtime package when present
3. bundled `libffmpeg_exec.so`
4. copied executable fallback from assets

This layered strategy is important because device/runtime behavior is not
perfectly uniform across Android versions and ABI layouts.

Relevant files:

- `app/src/main/java/com/localdownloader/downloader/BinaryInstaller.kt`
- `app/src/main/java/com/localdownloader/downloader/YtDlpExecutor.kt`
- `app/src/main/java/com/localdownloader/ffmpeg/FfmpegExecutor.kt`

---

## 3. Queue And Download State

The app currently treats queue state as a product-level feature, not a simple
"fire and forget" worker wrapper.

Current queue implementation includes:

- active/scheduled/paused/done/error/canceled views
- concurrency controls
- retry and cancellation actions
- per-task logs and diagnostics
- playlist item stability improvements
- guard rails around runtime updates while tasks are active

Relevant files:

- `app/src/main/java/com/localdownloader/data/DownloadTaskStore.kt`
- `app/src/main/java/com/localdownloader/data/DownloadRepositoryImpl.kt`
- `app/src/main/java/com/localdownloader/ui/screens/ProgressScreen.kt`
- `app/src/main/java/com/localdownloader/ui/screens/DownloadHistoryScreen.kt`

---

## 4. Cookies And YouTube Access

The repository no longer depends on the removed desktop helper path.

Current in-app approach:

1. user saves site cookies through the Cookies screen or targeted capture flows
2. YouTube access uses an in-app WebView-based login/generation path
3. the app exports matching cookie/session information locally
4. generated YouTube access data is reused in later recovery-prone requests

This implementation should still be treated as best-effort because upstream
YouTube and BotGuard behavior can change without notice.

Relevant files:

- `app/src/main/java/com/localdownloader/ui/screens/CookiesScreen.kt`
- `app/src/main/java/com/localdownloader/ui/screens/YoutubeAuthScreen.kt`
- `app/src/main/java/com/localdownloader/utils/WebViewCookieExporter.kt`
- `app/src/main/java/com/localdownloader/utils/YoutubePoTokenGenerator.kt`

---

## 5. Update System

The app includes a dedicated update subsystem for:

- app release checks
- `yt-dlp` runtime updates
- FFmpeg runtime updates

Main pieces:

- `updates/GitHubReleaseClient.kt`
- `updates/AppUpdateManager.kt`
- `updates/YtDlpUpdateManager.kt`
- `updates/FfmpegUpdateManager.kt`
- `viewmodel/UpdatesViewModel.kt`
- `worker/YtDlpUpdateScheduler.kt`
- `worker/YtDlpUpdateWorker.kt`

Manual runtime installs are intentionally guarded when downloads are active so
runtime replacement does not race against ongoing work.

---

## 6. Localization

The app now uses Android string resources as the real localization foundation.

That means new languages are no longer a screen-by-screen code rewrite. The
normal path is:

1. add locale resources
2. expose the locale in the language catalog
3. keep placeholders and plural blocks aligned

The App log reader intentionally remains mostly untranslated in content because
raw logs should stay readable and faithful to the original runtime output.

---

## 7. Stable UI Baseline

The current beta line treats the following as stable surfaces:

- Home / Browse
- Downloads
- More
- queue/history
- settings hub and settings subpages
- access tools
- help and updates pages

Future changes can still improve these screens, but large structural resets are
not the expected default path right now.

---

## 8. Remaining Practical Engineering Work

The highest-value future work is not a broad feature scramble. It is:

1. stronger download/runtime regression handling
2. more tests around queue and update flows
3. translation refinement
4. CI warning cleanup
5. ongoing documentation accuracy
