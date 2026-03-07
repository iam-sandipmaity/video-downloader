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
- Coordinates use cases
- Converts UI interactions into domain requests

## `domain` layer

- Stable models (`VideoInfo`, `MediaFormat`, `DownloadTask`, etc.)
- Repository interfaces
- Use cases for core operations

## `data` layer

- `DownloaderRepository` implementation
- WorkManager enqueue/cancel/resume orchestration
- In-memory queue store + DataStore-based settings

## `downloader` layer

- `BinaryInstaller` preferring packaged `jniLibs` binaries in `nativeLibraryDir` with ABI-based asset+chmod fallback
- `YtDlpExecutor` for process execution
- `FormatExtractor` for JSON parsing
- `DownloadEngine` for building yt-dlp commands
- `ProgressParser` for output parsing

## `ffmpeg` layer

- `FfmpegExecutor`
- `FormatConverter`
- `Compressor`
- `AudioExtractor`

## `worker` layer

- `DownloadWorker` executes queued downloads in background
- Foreground notification for long-running jobs
- Updates queue state used by UI

## Sequence (download path)

1. User pastes URL and taps Analyze.
2. ViewModel runs `AnalyzeUrlUseCase`.
3. Repository calls `FormatExtractor` (`yt-dlp -J ...`).
4. User selects format/options and taps Download.
5. ViewModel runs `StartDownloadUseCase`.
6. Repository enqueues `DownloadWorker`.
7. Worker invokes `DownloadEngine` -> `yt-dlp` locally.
8. Progress lines parsed and mapped to task state.
9. Result path saved in queue/history UI.

## Sequence (conversion/compression path)

1. UI issues conversion/compression request (future screen extension).
2. Repository calls FFmpeg wrappers.
3. `FfmpegExecutor` runs local binary.
4. Output file path returned to caller.

## Extensibility points

- Add custom filename template presets
- Add parallel download policy via WorkManager constraints
- Add persistent queue/history store (Room)
- Add richer FFmpeg profiles per platform
- Add runtime binary checksum verification
