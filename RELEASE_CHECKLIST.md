# Release Checklist

Use this checklist before tagging or publishing a new app release.

## Metadata

- confirm `APP_VERSION_CODE` is incremented
- confirm `APP_VERSION_NAME` matches the intended release
- confirm `CHANGELOG.md` has the correct release entry and date

## Build And Runtime

- run `gradle :app:assembleDebug`
- run unit tests when practical
- confirm update flows still gate correctly while downloads are active
- confirm no new runtime-selection regressions in `yt-dlp` or FFmpeg paths

## UI And Product Check

- verify Home / Browse still analyzes and opens the download sheet
- verify single-file download flow
- verify playlist download flow
- verify queue, history, and downloads screens
- verify Cookies and YouTube access screens
- verify converter and compressor basic flows

## Localization

- check supported languages for obvious fallback-to-English regressions
- check long titles, buttons, and empty states for clipping
- check plural/count strings where recent changes touched them

## Docs And Repo

- confirm README screenshots match the current UI
- confirm README / compatibility / implementation notes are still accurate
- confirm `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md`, and issue/PR templates are present

## Release Confidence

- review exported logs only if troubleshooting was needed
- verify no secrets, cookies, personal logs, or keystore files are staged
- verify the working tree is clean before tagging or pushing release commits
