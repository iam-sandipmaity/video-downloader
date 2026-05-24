# Updates, Runtimes, And Compatibility

This page explains why the app has an Updates center, how runtimes fit into the
product, and what device support looks like.

## Why Updates Matter So Much

Video Downloader depends on two fast-moving runtime pieces:

- `yt-dlp` for extractor and download behavior
- FFmpeg for merge, remux, conversion, and compression work

That means site compatibility can change even when the APK itself did not.

## Updates Center

The in-app Updates flow exists to manage:

- app updates
- `yt-dlp` runtime updates
- FFmpeg runtime updates

This is not extra clutter. It is a core part of keeping the downloader useful.

## App Updates

App updates are for:

- new features
- UI changes
- bug fixes
- security hardening
- deeper downloader or storage changes

Stable releases:

- `https://github.com/iam-sandipmaity/video-downloader/releases`

Nightly builds:

- `https://nightly.link/iam-sandipmaity/video-downloader/workflows/android-build.yml/main`

## `yt-dlp` Runtime Updates

Runtime updates are especially important when:

- a site used to work but now fails
- a page now returns fewer formats
- the extractor falls back to a generic unsupported path
- upstream website behavior changed

The app now uses `yt-dlp`'s own self-update path for runtime updating, which is
more aligned with how the runtime expects to move between versions.

## FFmpeg Runtime Updates

FFmpeg matters when the app needs to:

- merge split streams
- remux containers
- convert media
- compress media
- recover from some postprocessing edge cases

You may not notice FFmpeg directly on every download, but you notice it when
media compatibility gets harder.

## Why Updates Can Be Disabled Temporarily

Runtime installs and replacements are intentionally guarded when active
downloads would make replacement unsafe.

That means update actions can be blocked while:

- downloads are running
- downloads are paused in a way that still depends on active state
- queued work would make runtime replacement risky

This is deliberate safety behavior.

## Android Version Support

Supported Android versions:

- Android 8.0 / 8.1 as the minimum
- Android 9 through Android 15 with full support targets in current docs

For the full matrix:

- `https://github.com/iam-sandipmaity/video-downloader/blob/main/COMPATIBILITY.md`

## CPU Architecture Support

Default shipped ABI:

- `arm64-v8a`

Other ABIs are possible with custom builds, but they are not the default public
release target.

## Runtime Resolution Philosophy

The app uses layered runtime handling because Android devices do not all behave
the same way.

High-level strategy:

- embedded `youtubedl-android` path for `yt-dlp`
- layered FFmpeg resolution with managed and fallback options

This is a reliability decision, not accidental complexity.

## Public Download Location

Default public root:

```text
Download/LocalDownloader/
```

The app may still use app-owned storage for temporary or runtime-managed work
behind the scenes.

## When To Check Updates First

Before filing a bug, it is smart to check Updates when:

- a single site changed behavior recently
- analysis suddenly fails on previously working links
- available resolutions look lower than before
- a runtime-specific feature stopped working

## Related Docs

- Compatibility:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/COMPATIBILITY.md`
- Changelog:
  `https://github.com/iam-sandipmaity/video-downloader/blob/main/CHANGELOG.md`
- Release notes folder:
  `https://github.com/iam-sandipmaity/video-downloader/tree/main/docs/releases`

## Related Wiki Pages

- [Troubleshooting](Troubleshooting.md)
- [Settings, Cookies, and YouTube Access](Settings-Cookies-and-YouTube-Access.md)
- [FAQ](FAQ.md)
