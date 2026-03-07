# Development Guide

## Prerequisites

- JDK 17
- Android SDK + platform tools
- Gradle available in PATH (or add wrapper)

## Setup

1. Clone repo.
2. Add platform binaries under `app/src/main/assets/yt-dlp/*` and `app/src/main/assets/ffmpeg/*`.
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
3. Persist default in `SettingsStore` if needed.
4. Add CLI mapping in `DownloadEngine`.
5. Add UI toggle/input.

## Adding new FFmpeg operations

1. Add domain request model if needed.
2. Add wrapper in `ffmpeg/`.
3. Expose operation through repository.
4. Add use case and UI action.

## Debugging binary issues

- Verify `jniLibs` binary exists for active ABI (`libyt_dlp.so` / `libffmpeg_exec.so`).
- Confirm app uses `nativeLibraryDir` binary first (see `BinaryInstaller` logs).
- If native lib is missing, verify asset path exists for fallback copy.
- If fallback copy is used, ensure copied binary in `files/bin` is executable.
- Inspect `stderr` captured in `CommandResult`.
- Confirm command args by checking logcat (`YtDlpExecutor`, `FfmpegExecutor`).

## Recommended next steps for production hardening

- Persist queue/history in Room
- Add retry/backoff policy per task type
- Add notification actions (pause/resume/cancel)
- Add signed binary verification (hash check)
- Add integration tests around command generation
