# Architecture

## Goals

- keep download and media-processing execution local to the Android device
- preserve maintainable layering with explicit responsibilities
- allow runtime, queue, and translation improvements without repeatedly
  rebuilding the app's core screen structure

## Layers

### `ui`

- Compose screens and reusable components
- user input, navigation, dialogs, and presentation
- no direct downloader/runtime execution

### `viewmodel`

- owns screen state as flows/state objects
- turns UI intent into repository and manager actions
- coordinates download, media-tool, settings, and update behavior

### `domain`

- stable models and contracts
- shared request/response types
- repository interfaces

### `data`

- repository implementations
- queue/task persistence and orchestration
- settings persistence
- app-library bookkeeping

### `downloader`

- runtime selection
- command planning
- process execution
- progress parsing
- analysis parsing

### `ffmpeg`

- conversion and compression wrappers
- local FFmpeg execution
- media-tool request handling

### `updates`

- release metadata retrieval
- app/runtime update decision logic
- install preparation and guarded replacement behavior

### `worker`

- background download execution
- background update scheduling
- long-running foreground service-style work integration through WorkManager

## Primary Execution Paths

### Download path

1. user analyzes a link from Home / Browse
2. ViewModel requests analysis
3. repository uses `FormatExtractor`
4. user confirms options in the download sheet
5. repository schedules a task
6. `DownloadWorker` executes the task
7. progress is parsed and surfaced into queue/history/downloads state

### Media-tool path

1. user selects a local file in Converter or Compressor
2. UI builds a conversion/compression request
3. repository calls FFmpeg wrappers
4. `FfmpegExecutor` runs locally
5. saved output is surfaced back into the library/UI

### Update path

1. user opens Updates or background scheduling triggers a check
2. update managers load release/runtime state
3. GitHub metadata is fetched
4. installs are gated when active downloads would make replacement unsafe

## Runtime Philosophy

The app intentionally uses layered fallbacks for FFmpeg and a managed embedded
runtime path for `yt-dlp` because Android packaging/runtime behavior is not
uniform across all devices.

This is a reliability choice, not accidental complexity.

## Product Stability Direction

The current beta line treats the user-facing screen structure as stable enough
that most future work should happen inside the existing architecture:

- queue logic improvements
- runtime reliability improvements
- translation/resource improvements
- docs and test improvements

That keeps user-facing churn low while allowing the internal implementation to
keep getting stronger.
