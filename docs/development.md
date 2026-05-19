# Development Guide

## Prerequisites

- JDK 17
- Android SDK + platform tools
- Gradle available in PATH (or add wrapper)

## Setup

1. Clone repo.
2. If you are building for a custom FFmpeg ABI, add binaries under `app/src/main/assets/ffmpeg/*` and `app/src/main/jniLibs/*` as described in `COMPATIBILITY.md`.
3. Open in Android Studio or build from terminal.

## Build commands

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## Coding standards used in this project

- Keep files small and focused
- Use constructor injection everywhere practical
- Separate command construction from command execution
- Keep UI declarative and state-driven
- Keep command I/O parsing centralized (`ProgressParser`)

## Adding new yt-dlp options

1. Add field to `DownloadOptions`.
2. Map field in `FormatViewModel`.
3. Persist the default in `AppSettings` and `SettingsStore` if needed.
4. Add CLI mapping in `DownloadEngine`.
5. Add UI toggle/input.

## Working on update flows

- Update release-channel or preference models in `updates/UpdateModels.kt`.
- Keep UI wiring in `viewmodel/UpdatesViewModel.kt`.
- Use `updates/GitHubReleaseClient.kt` for GitHub metadata or asset downloads.
- Background yt-dlp maintenance belongs in `worker/YtDlpUpdateScheduler.kt` and `worker/YtDlpUpdateWorker.kt`.
- Guard runtime replacement when downloads are active; current behavior intentionally blocks manual runtime installs in that case.

## Adding new FFmpeg operations

1. Add domain request model if needed.
2. Add wrapper in `ffmpeg/`.
3. Expose operation through repository.
4. Add use case and UI action.

## Debugging binary issues

- Confirm whether the app is using a managed FFmpeg overlay, embedded runtime package, bundled `libffmpeg_exec.so`, or copied asset fallback (see `BinaryInstaller` logs).
- Verify the fallback asset exists for the active ABI under `app/src/main/assets/ffmpeg/<abi>/ffmpeg` if you are building custom ABI support.
- If fallback copy is used, ensure the copied binary in app-owned storage is executable.
- For yt-dlp issues, inspect the runtime version shown in the app's Updates screen or by running `--version` through `YtDlpExecutor`.
- Inspect `stderr` captured in `CommandResult`.
- Confirm command args by checking logcat (`YtDlpExecutor`, `FfmpegExecutor`).

## Recommended next steps for production hardening

- Persist queue/history in Room
- Add retry/backoff policy per task type
- Add notification actions (pause/resume/cancel)
- Add signed binary verification (hash check)
- Add integration tests around command generation
