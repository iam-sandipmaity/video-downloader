# Library, Playback, And Media Tools

The app is not only a downloader. It also keeps downloaded media useful after
the task finishes.

## Downloads Library

The Downloads area is the main local-media surface of the app.

It helps you:

- browse completed files
- inspect saved media entries
- open files in built-in players
- share files
- delete files
- use batch actions on selected items

The library is meant to be a practical continuation of the download workflow,
not a dead-end receipt screen.

## Saved File Viewer

When you focus on a specific file, the viewer can make it easier to:

- confirm the saved item
- open playback
- inspect local context
- manage the file without leaving the app

## Built-In Audio Player

The audio player is designed more like a small music player than a raw preview.

Common behavior includes:

- queue playback
- play or pause
- seek controls
- previous and next
- shuffle
- repeat modes
- sleep timer
- background playback and notification-style controls

This makes audio-only downloads much easier to use as real saved media.

## Built-In Video Player

The video player is focused on local playback of downloaded files.

Available behavior can include:

- full-screen playback
- gesture-based controls
- timeline seeking
- subtitle and audio-track selection when present
- playback speed choices
- resize or fit modes
- picture-in-picture for video playback where supported
- volume boost and playback settings

Recent playback polish also keeps the center play button visually centered and
improves handoff behavior with the app's own audio player.

## Playback Handoff

The app now tries to avoid making its own audio and video players compete.

Practical behavior:

- opening video playback pauses the app's background audio queue
- starting the app's audio queue pauses active video playback from the app

That keeps playback more predictable inside the app itself.

## Converter

The converter is useful when a download finished successfully but you still want
another output format afterward.

Typical use cases:

- convert video to another container
- convert audio to a more convenient format
- create a simpler output for sharing

## Compressor

The compressor is for reducing file size rather than just changing the wrapper.

Typical use cases:

- preparing a smaller file for sharing
- reducing storage use
- creating a lighter local copy

Compression usually trades off some quality for smaller output size.

## Good Library Workflow Patterns

Examples of useful in-app flows:

- download a lecture, then play it in the audio player
- download a video, then compress it for messaging apps
- save a file, verify it in the viewer, and share it from the library
- use the built-in players first before reaching for another app

## When To Use Another Player

The built-in players are meant to cover the main use cases, but another player
may still make sense when:

- you need a feature the built-in player does not yet expose
- you prefer an external playback app
- you are testing raw file compatibility outside the app

## Related Pages

- [Download Workflow](Download-Workflow.md)
- [Getting Started](Getting-Started.md)
- [Troubleshooting](Troubleshooting.md)
