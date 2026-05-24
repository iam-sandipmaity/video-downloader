# FAQ

## Does the app use its own backend server?

No. The app is designed as a local-first Android downloader. Analysis,
downloads, and most media processing happen on your device.

## Do I need an account to use the app?

No project-owned account is required.

## What powers the downloader?

The app relies on `yt-dlp` for extraction and download behavior, and FFmpeg for
merge, remux, conversion, and compression work.

## Which Android versions are supported?

Android 8.0 and newer.

## Which CPU architecture is shipped by default?

`arm64-v8a`

## Does it support playlists?

Yes. Playlist downloads can use global defaults and per-item overrides.

## Can I download audio-only files?

Yes. Audio-only workflows are supported, and completed audio can be played in
the built-in audio player.

## Can I rename a file before downloading?

Yes. The app supports pre-download naming control.

## Where are files saved?

Usually under:

```text
Download/LocalDownloader/
```

The exact folder can be changed from settings.

## Why do format lists change over time?

Because the site, authentication state, or extractor behavior may have changed.
A different runtime version can also expose different results.

## Why does a site work in one release and fail later?

That can happen because:

- the site changed
- the extractor changed
- the page now needs cookies
- the runtime needs updating

It is not always caused by the APK alone.

## Do I always need cookies?

No. Many downloads work fine without them. Cookies are mainly for tougher
authenticated or login-dependent cases.

## What is the YouTube access flow for?

It is an in-app recovery path for harder YouTube request conditions. Most users
should only use it when normal requests or normal cookies are not enough.

## Does the app support subtitles?

Yes, where the source and workflow support them. Subtitle behavior depends on
what the site exposes and what options you selected.

## Can I update `yt-dlp` without installing a full new APK?

Yes. The app has an Updates flow for runtime updates.

## Can I update FFmpeg without installing a full new APK?

Yes, where the app's managed FFmpeg runtime flow is available.

## Why are some update actions blocked?

For safety. The app blocks some runtime replacements while active downloads
could make replacement unsafe.

## Does the app keep a download queue?

Yes. Queue management is a major part of the app, including retries,
diagnostics, and status tracking.

## Does the app have its own media library?

Yes. Completed downloads can be browsed from the library and opened in built-in
players.

## Can the app play audio in the background?

Yes. The built-in audio player supports background playback behavior.

## Can the app play video in picture-in-picture?

Video playback supports picture-in-picture where the device and flow allow it.

## Does the app try to stop audio and video from playing over each other?

Yes. The app coordinates its own players so starting one playback path can
pause the other.

## What should I do before opening a bug report?

Usually:

1. retry once
2. update the runtime
3. test whether cookies are required
4. capture the exact error
5. report the issue with clean reproduction steps

## Where do I read release changes?

- Changelog:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/CHANGELOG.md`
- Releases:
  `https://github.com/iam-sandipmaity/video-downloader/releases`

## Where can I file issues?

`https://github.com/iam-sandipmaity/video-downloader/issues`
