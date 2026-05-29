# Build And Development

This page is for contributors and maintainers working from source.

## Prerequisites

- JDK 17
- Android SDK and platform tools
- use the bundled Gradle wrapper from the repository root

## Basic Setup

```bash
git clone https://github.com/iam-sandipmaity/video-downloader
cd video-downloader
./gradlew :app:assembleStandardDebug
```

Use `gradlew.bat` instead of `./gradlew` when running from PowerShell or Command Prompt on Windows.

Useful test command:

```bash
./gradlew :app:testStandardDebugUnitTest
```

## Current Development Posture

The project currently treats the `1.7.2` UI structure as a stable baseline.
That means the best work usually falls into these groups:

- fixing downloader or runtime regressions
- tightening queue and recovery behavior
- improving docs
- expanding tests
- refining translations
- hardening update and storage flows

## Recommended Local Workflow

1. build the debug APK
2. reproduce the issue on-device or emulator
3. make the smallest fix that clearly solves the problem
4. add or update tests when possible
5. re-check user-facing behavior for regressions

## Useful Commands

```bash
./gradlew :app:assembleStandardDebug
./gradlew :app:compileStandardDebugKotlin
./gradlew :app:testStandardDebugUnitTest
./gradlew :app:lintStandardDebug
```

## Repository Structure At A Glance

| Area | Purpose |
| --- | --- |
| `app/src/main/java/com/localdownloader/ui` | Compose screens and presentation |
| `app/src/main/java/com/localdownloader/viewmodel` | UI state and orchestration |
| `app/src/main/java/com/localdownloader/data` | repositories, persistence, scheduling |
| `app/src/main/java/com/localdownloader/downloader` | analysis, command planning, execution |
| `app/src/main/java/com/localdownloader/ffmpeg` | conversion and compression wrappers |
| `app/src/main/java/com/localdownloader/updates` | app/runtime update logic |
| `app/src/main/java/com/localdownloader/worker` | WorkManager execution |

## Practical Code Guidelines

- keep command construction separate from execution
- keep UI state-driven and declarative
- preserve the local-first model
- prefer clear reliability fixes over clever shortcuts
- avoid changing stable UI structure unless there is a strong reason

## Working On Download Features

Typical path:

1. update the option model
2. wire the state through the relevant ViewModel
3. persist the default if needed
4. map the option into the downloader command path
5. expose it in the correct screen or sheet

## Working On Localization

Translations are managed through Hosted Weblate:
`https://hosted.weblate.org/projects/local-video-downloader/android-app-strings/`.
The Weblate component reads the base Android XML file at
`app/src/main/res/values/strings.xml` and writes translated Android XML files
back into the matching `values-<locale>` resource folders.

Keep the old Crowdin workflow disabled in `master.yml`. Only one localization
platform should write translation updates to the repository at a time.

When touching translations manually:

- keep keys aligned with `values/strings.xml`
- preserve placeholders such as `%1$s` and `%1$d`
- review plurals
- avoid translating raw technical log output when fidelity matters more
- remove translated keys from the matching locale `strings_lint_fillins.xml`
  file if they were previously present only as lint fallbacks

If adding a new language, also update:

- the language catalog
- locale configuration

Maintainers should keep the Weblate component configured with:

- source repository: `https://github.com/iam-sandipmaity/video-downloader.git`
- branch: `main`
- file format: Android String Resource
- file mask: `app/src/main/res/values-*/strings.xml`
- monolingual base language file: `app/src/main/res/values/strings.xml`

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

Keep runtime installs guarded when active downloads would make replacement
unsafe.

## Working On Runtime Or Binary Issues

Check:

- which FFmpeg path `BinaryInstaller` selected
- whether the active ABI has the expected fallback assets
- whether the copied runtime is executable
- whether the Updates flow shows the expected runtime version
- `stderr` captured in command results
- app logs and per-task diagnostics

## Good Near-Term Engineering Targets

- queue and update test coverage
- runtime warning cleanup
- download recovery fixes
- translation polish
- documentation accuracy

## Related Repository Docs

- Development guide:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/docs/development.md`
- Contributing:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/CONTRIBUTING.md`
- Release checklist:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/RELEASE_CHECKLIST.md`
