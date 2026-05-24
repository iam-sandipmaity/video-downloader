# Download Workflow

This page explains how a download normally moves through the app, from pasted
link to saved file.

## 1. Analyze The Link

The workflow starts on Home or through a shared link:

1. paste or share the URL into the app
2. let the app analyze it locally with `yt-dlp`
3. wait for available metadata and format choices

During analysis, the app is trying to understand:

- title
- duration
- thumbnail
- playlist structure if present
- available video and audio formats
- subtitle or track information when available

## 2. Pick The Download Type

Common download styles are:

- video with audio
- video-only
- audio-only
- playlist download

Which one makes sense depends on what the site exposes and what you want to do
later with the file.

## 3. Choose Format And Container

The download sheet can expose multiple choices depending on the source.

Things you may see:

- height or quality labels like `720p`, `1080p`, or better
- codec hints
- container hints like `mp4`, `webm`, or `m4a`
- merged formats that already include audio
- split formats that need local merge work after download

Practical advice:

- `Auto` is the safest default when you do not care about exact container rules
- explicit `mp4` is useful when broad playback compatibility matters
- source-specific formats can vary a lot, even for the same site over time

## 4. Name The Output

Before you start the task, you can usually adjust:

- final file name
- output template behavior
- folder routing through your saved defaults

For playlist downloads, the app can keep source titles or let you override
items more precisely.

## 5. Optional Extras

Depending on the media and your defaults, the app may also handle:

- subtitles
- thumbnails
- metadata embedding
- audio extraction
- postprocessing with FFmpeg

Not every site or format supports all of these equally well.

## 6. Queue The Task

After confirmation, the app turns your choices into a queued task and hands it
to the worker-based download system.

The queue is not just cosmetic. It is where you manage:

- scheduled work
- active work
- retries
- paused items
- failed items
- diagnostics

## 7. Runtime Execution

The actual execution path usually looks like this:

1. the app calls local `yt-dlp`
2. media data is fetched
3. split streams are downloaded when needed
4. FFmpeg is used for merge, remux, or follow-up processing when required
5. the result is saved into the configured download area

Because this all happens locally, device performance, storage, and network
quality matter.

## 8. Follow Progress

While a task is running, you can usually:

- watch progress from the queue
- pause or cancel
- inspect task status
- review errors if the task fails

The app also keeps history and local logs so recovery is not guesswork.

## 9. Open The Result

After a successful task, you can:

- open it from the Downloads library
- play it in the built-in audio player
- play it in the built-in video player
- share it
- use it in the converter or compressor

## Single Download Tips

- use `Auto` if you want a good result without micromanaging the format tree
- rename the file before starting if the source title is messy
- only use cookies when the site actually needs them

## Playlist Tips

- set a global default format first
- only override items when needed
- keep an eye on queue size if the playlist is large
- use retries selectively for the items that actually failed

## Why Formats Change Over Time

If you notice fewer formats or different choices than before, it does not
always mean the app regressed. Common reasons include:

- the site changed what it exposes
- the extractor behavior changed upstream
- authentication context changed
- you need a newer `yt-dlp` runtime
- the source page now provides different manifests

That is why runtime updates and cookies are first-class features in the app.

## If A Download Fails

Start here:

- [Troubleshooting](Troubleshooting.md)
- [Updates, Runtimes, and Compatibility](Updates-Runtimes-and-Compatibility.md)
- [Settings, Cookies, and YouTube Access](Settings-Cookies-and-YouTube-Access.md)
