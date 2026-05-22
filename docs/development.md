# Development Guide

## Prerequisites

- JDK 17
- Android SDK and platform tools
- Gradle available in `PATH`

## Setup

```bash
git clone https://github.com/iam-sandipmaity/video-downloader
cd video-downloader
gradle :app:assembleDebug
```

If you are working on custom FFmpeg ABI packaging, also review
[../COMPATIBILITY.md](../COMPATIBILITY.md).

## Common Commands

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## Working Style For This Repo

This project is currently in a stable-beta phase. That means most good work
falls into one of these groups:

- fixing downloader/runtime regressions
- tightening queue and recovery behavior
- improving docs or translation quality
- adding targeted tests
- refining internal logic without disrupting the stable UI structure

## Practical Code Guidelines

- keep user-visible behavior stable unless the change solves a real problem
- keep command construction separate from command execution
- keep UI state-driven and declarative
- preserve the local-first execution model
- prefer clear, maintainable fixes over clever shortcuts

## Adding New Download Options

Typical path:

1. add a field to the relevant options model
2. wire it through the ViewModel state
3. persist it in settings if it is a default
4. map it into the downloader command path
5. expose it in UI only where it belongs

## Working On Localization

When adding or updating translations:

1. keep keys aligned with `app/src/main/res/values/strings.xml`
2. preserve placeholders like `%1$d` and `%1$s`
3. review plural blocks too, not just plain strings
4. leave raw log content and some technical labels untranslated when accuracy
   matters more than localization

If a new language is added structurally, also update:

- the app language catalog
- locale config

## Working On Update Flows

Relevant files:

- `updates/UpdateModels.kt`
- `updates/GitHubReleaseClient.kt`
- `updates/AppUpdateManager.kt`
- `updates/YtDlpUpdateManager.kt`
- `updates/FfmpegUpdateManager.kt`
- `viewmodel/UpdatesViewModel.kt`
- `worker/YtDlpUpdateScheduler.kt`
- `worker/YtDlpUpdateWorker.kt`

Keep manual runtime installs guarded when downloads are active.

## Debugging Binary Or Runtime Issues

Check:

- which FFmpeg path was selected by `BinaryInstaller`
- whether the fallback asset exists for the active ABI
- whether the copied fallback binary is executable when used
- the runtime version shown by the Updates flow
- `stderr` captured in `CommandResult`
- runtime/download logs in `YtDlpExecutor` and `FfmpegExecutor`

## YouTube Access Notes

The current PO-token path is best-effort and upstream-sensitive.

When touching it:

- keep WebView session, cookies, and generated values aligned
- verify the saved recovery data still matches the request path that uses it
- document any upstream constant refreshes clearly

## Good Near-Term Engineering Targets

- queue/update test coverage
- runtime warning cleanup
- download recovery fixes
- translation polish
- documentation accuracy
