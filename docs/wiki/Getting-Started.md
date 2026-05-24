# Getting Started

This page is the fastest path from install to your first successful download.

## Install The App

Recommended sources:

- Stable releases:
  `https://github.com/iam-sandipmaity/video-downloader/releases`
- Obtainium:
  `https://github.com/iam-sandipmaity/video-downloader`
- Nightly builds:
  `https://nightly.link/iam-sandipmaity/video-downloader/workflows/android-build.yml/main`

Requirements:

- Android 8.0 or newer
- ideally an `arm64-v8a` device

## First Launch Checklist

On first launch, it is worth checking a few basics:

- confirm the app can show notifications if you want queue status updates
- review the default download folder path in Settings
- decide whether you want Wi-Fi-only downloads
- leave cookies and YouTube access alone unless a site actually needs them

## Your First Download

1. Open the Home screen.
2. Paste a media link into the input field.
3. Wait for the app to analyze the link locally.
4. Choose a format and output style.
5. Adjust the file name if needed.
6. Tap download.
7. Follow progress from the queue screen.

After the task finishes, open it from the Downloads library or file viewer.

## Single Item Vs Playlist

If the link is a single media item, you usually only need:

- format choice
- container preference
- rename choice

If the link is a playlist, you can usually control:

- which items are included
- a shared default format
- per-item format overrides
- per-item rename overrides

## Good Starter Defaults

If you do not want to fine-tune everything on day one, these defaults are
usually sensible:

- let the app use `Auto` container behavior
- keep Wi-Fi-only downloads enabled if you have limited data
- leave subtitles disabled unless you need them
- leave cookies empty until a site asks for sign-in or returns poorer results

## Where Files Go

Default public save root:

```text
Download/LocalDownloader/
```

The app can also create or use media-specific folders depending on your
settings.

## When To Use Cookies

Cookies are useful when:

- a site only shows full media details to signed-in users
- analysis works but some formats are missing
- downloads fail with login or session-related errors

Do not import cookies casually. They can grant account or session access.

## When To Use YouTube Access Tools

The YouTube access flow exists for tougher YouTube-side request conditions.
Most casual downloads will not need it, but it can help when:

- normal YouTube requests fail unexpectedly
- a newer runtime still needs better authenticated context
- upstream protection behavior changed

## If Something Goes Wrong

Start here:

- [Troubleshooting](Troubleshooting.md)
- [FAQ](FAQ.md)
- [Updates, Runtimes, and Compatibility](Updates-Runtimes-and-Compatibility.md)

If a site used to work and suddenly behaves worse, check the Updates page in
the app before assuming the whole app is broken.
