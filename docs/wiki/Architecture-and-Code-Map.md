# Architecture And Code Map

This page gives a maintainable overview of the app's major layers and execution
paths.

## Main Goals

- keep downloading and media processing local to the Android device
- preserve clear layering with explicit responsibilities
- improve runtime reliability without constantly rebuilding the whole UI model

## Layer Map

| Layer | Purpose |
| --- | --- |
| `ui` | Compose screens, dialogs, interactions, presentation |
| `viewmodel` | screen state, flows, orchestration, user-intent handling |
| `domain` | stable models and contracts |
| `data` | repository implementations, persistence, scheduling |
| `downloader` | link analysis, command planning, runtime execution |
| `ffmpeg` | conversion, compression, merge-related execution |
| `updates` | app and runtime update logic |
| `worker` | background execution and WorkManager integration |

## Primary Execution Paths

### Download Path

1. user pastes or shares a link
2. `FormatViewModel` requests analysis
3. the repository uses `FormatExtractor`
4. the user confirms format and output choices
5. a task is scheduled through the repository layer
6. `DownloadWorker` executes the work
7. queue, history, and downloads state are updated

Key files:

- `viewmodel/FormatViewModel.kt`
- `downloader/FormatExtractor.kt`
- `downloader/DownloadEngine.kt`
- `worker/DownloadWorker.kt`

### Media Tool Path

1. user selects a local file
2. UI builds a conversion or compression request
3. repository and FFmpeg wrappers handle the job
4. `FfmpegExecutor` runs locally
5. output is surfaced back into the library

Key files:

- `ffmpeg/FfmpegExecutor.kt`
- `ffmpeg/FormatConverter.kt`
- `ffmpeg/Compressor.kt`

### Update Path

1. user opens Updates or background scheduling runs
2. update managers load current state
3. GitHub metadata is fetched
4. installs are guarded when active downloads make replacement unsafe

Key files:

- `updates/AppUpdateManager.kt`
- `updates/YtDlpUpdateManager.kt`
- `updates/FfmpegUpdateManager.kt`
- `worker/YtDlpUpdateScheduler.kt`

## Queue And Task State

Queue state is treated as a core product feature rather than a thin wrapper.

Current direction includes:

- active, queued, paused, failed, canceled, and completed views
- retry flows
- diagnostics
- playlist handling
- safety gates around updates while tasks are active

Important files:

- `data/DownloadTaskStore.kt`
- `data/DownloadRepositoryImpl.kt`
- `ui/screens/ProgressScreen.kt`
- `ui/screens/DownloadHistoryScreen.kt`

## Runtime Philosophy

The app intentionally uses layered fallbacks for FFmpeg and a managed embedded
path for `yt-dlp`.

Why:

- Android packaging is not identical across all devices
- site compatibility shifts often
- postprocessing needs can vary by source and format

This is a reliability strategy, not accidental complexity.

## Playback Model

The app has separate audio and video playback surfaces for local media:

- audio playback is queue-oriented and background-friendly
- video playback is focused on full-screen local playback
- playback coordination now helps prevent the app's own players from competing

## Good Places To Start Debugging

If the issue is mostly about analysis:

- `FormatExtractor.kt`
- related selector or request-planning helpers

If the issue is mostly about download execution:

- `DownloadEngine.kt`
- `DownloadWorker.kt`
- `YtDlpExecutor.kt`

If the issue is mostly about merge or conversion:

- `FfmpegExecutor.kt`
- FFmpeg wrappers

If the issue is mostly about runtime updating:

- update managers
- release metadata clients
- update workers

## Stable UI Baseline

The project currently treats these surfaces as stable enough that most work
should improve them rather than replace them:

- Home and Browse
- Downloads
- queue and history
- More
- settings hub and subpages
- updates and help
- access tools

## Related Docs

- Architecture doc:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/docs/architecture.md`
- Implementation notes:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/IMPLEMENTATION.md`
- Development guide:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/docs/development.md`
