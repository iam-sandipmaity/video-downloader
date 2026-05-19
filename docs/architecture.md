# Architecture

## Goals

- Keep all download and conversion execution local to Android device
- Provide maintainable layering with explicit contracts
- Make future feature additions (new screens, repository backends, command flags) low-risk

## Layers

## `ui` layer

- Compose screens/components
- Stateless where possible
- Emits user intent to ViewModels
- Does not execute downloader commands directly

## `viewmodel` layer

- Owns UI state as `StateFlow`
- Coordinates repository calls and update managers
- Converts UI interactions into download, media-tool, and update requests

## `domain` layer

- Stable models (`VideoInfo`, `MediaFormat`, `DownloadTask`, etc.)
- Repository interfaces
- Shared request/response models for downloader and settings flows

## `data` layer

- `DownloaderRepository` implementation
- WorkManager enqueue/cancel/resume orchestration
- Room-backed task/history persistence plus in-memory task state helpers
- DataStore-based settings and app defaults

## `downloader` layer

- `BinaryInstaller` preferring managed FFmpeg overlays, then packaged native runtimes, then asset-installed fallback executables
- `YtDlpExecutor` for process execution
- `FormatExtractor` for JSON parsing
- `DownloadEngine` for building yt-dlp commands
- `ProgressParser` for output parsing

## `ffmpeg` layer

- `FfmpegExecutor`
- `FormatConverter`
- `Compressor`
- `AudioExtractor`

## `updates` layer

- `AppUpdateManager` for GitHub APK release checks and installer handoff
- `YtDlpUpdateManager` for replacing the embedded yt-dlp runtime
- `FfmpegUpdateManager` for managed FFmpeg overlay packages
- `GitHubReleaseClient` for release metadata and asset downloads

## `worker` layer

- `DownloadWorker` executes queued downloads in background
- `YtDlpUpdateWorker` performs guarded background yt-dlp update attempts
- Foreground notification for long-running jobs
- Schedulers and workers update queue/runtime state used by UI

## Sequence (download path)

1. User pastes URL and taps Analyze.
2. `FormatViewModel` requests analysis through the repository.
3. Repository calls `FormatExtractor` (`yt-dlp -J ...`).
4. User selects format/options and taps Download.
5. ViewModel builds `DownloadOptions` and calls the repository directly.
6. Repository enqueues `DownloadWorker`.
7. Worker invokes `DownloadEngine` -> `yt-dlp` locally.
8. Progress lines parsed and mapped to task state.
9. Result path saved in queue/history UI.

## Sequence (conversion/compression path)

1. UI issues conversion/compression request (future screen extension).
2. Repository calls FFmpeg wrappers.
3. `FfmpegExecutor` runs local binary.
4. Output file path returned to caller.

## Sequence (update path)

1. User opens the Updates screen or startup scheduling triggers a background check.
2. `UpdatesViewModel` loads preferences and asks the relevant update manager for release state.
3. `AppUpdateManager`, `YtDlpUpdateManager`, or `FfmpegUpdateManager` queries GitHub release metadata.
4. Manual installs are blocked while active downloads are running for runtime safety.
5. `YtDlpUpdateScheduler` can enqueue `YtDlpUpdateWorker` on startup when auto-update is enabled.

## Extensibility points

- Add custom filename template presets
- Add richer release channels or mirrors for runtime packages
- Add app-update integrity verification or signing checks
- Add more granular background scheduling policies for maintenance work
- Add richer FFmpeg profiles per platform
- Add runtime binary checksum verification
