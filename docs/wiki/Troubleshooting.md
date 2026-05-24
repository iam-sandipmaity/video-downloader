# Troubleshooting

This page covers the most common failure patterns and the fastest recovery
steps.

## Start With These Questions

Before going deep, check:

- does the link still open normally in a browser?
- did this site work before and only recently start failing?
- are you signed in on the site when that matters?
- is the app runtime up to date?
- are you on a weak network?
- is the failure during analysis, download, merge, or playback?

## 1. The Link Will Not Analyze

Try this first:

- check that the URL is valid and complete
- retry once on a stable network
- update the `yt-dlp` runtime from the Updates page
- use cookies if the site requires sign-in

If the page is still unsupported, it may be:

- a site `yt-dlp` does not support well yet
- a site that changed recently
- a page requiring authentication context

## 2. The Site Shows Fewer Formats Than Before

This can happen even when the app is working correctly.

Common causes:

- the source changed what it exposes
- the page now requires sign-in
- your previous runtime was newer or had a different extractor state
- the source is now giving a different manifest

Try:

- updating `yt-dlp`
- adding cookies for that site
- checking whether the browser itself still shows higher-quality playback

## 3. The Download Starts But Fails Later

That usually means analysis succeeded but one of the later stages failed.

Possible stages:

- media fetch
- stream download
- postprocessing
- file move or export

Try:

- retrying the task from the queue
- watching the exact failure message in history or diagnostics
- checking free storage space
- checking whether the chosen format requires a tougher merge path

## 4. The App Says `Unsupported URL`

This usually means the runtime fell back to a generic path that still could not
handle the page as a known extractor target.

Try:

- updating `yt-dlp`
- checking whether the page needs sign-in
- comparing behavior with a current external `yt-dlp` build if you are testing
  compatibility

## 5. YouTube Needs More Than A Normal Request

If YouTube behaves worse than expected:

- update the runtime first
- then use cookies if needed
- then use the YouTube access flow only if ordinary authenticated context still
  is not enough

Do not start with the most advanced recovery path unless you actually need it.

## 6. Playlist Downloads Feel Messy

Try simplifying the first pass:

- choose a global format
- avoid many per-item overrides at once
- test a small subset first

If only some items fail, treat those as source-specific exceptions rather than
assuming the whole playlist flow is broken.

## 7. Merge Or Postprocessing Fails

This happens when:

- selected streams do not fit the requested final container well
- the runtime needs FFmpeg help after separate downloads
- source codecs are awkward for the requested output

Try:

- use `Auto` container behavior
- retry with a different final format choice
- update FFmpeg if the app offers a managed runtime update

## 8. Files Downloaded But I Cannot Find Them

Check:

- the Downloads library inside the app
- the file viewer entry for the finished task
- your configured public download folder
- whether the device fell back to app-owned storage behavior

Default public root:

```text
Download/LocalDownloader/
```

## 9. Updates Are Disabled

This is often intentional.

The app blocks some runtime installs while:

- downloads are active
- queue state would make replacement unsafe

Finish, cancel, or stabilize active tasks first.

## 10. Video And Audio Playback Compete

The app now coordinates its own players better, but if playback still feels
wrong:

- stop the in-app audio queue before testing video
- retest after reopening the player
- confirm whether the conflict is with another app or only inside this app

## 11. Playback Issues With A Saved File

Try to separate playback from download integrity:

- test the file in the built-in player
- test the same file in another player
- confirm whether audio, video, subtitles, or container compatibility is the
  actual problem

If the file itself is good but playback is odd, it may be a player issue rather
than a downloader issue.

## 12. What To Collect Before Filing An Issue

Helpful details:

- app version
- Android version
- device model
- exact page URL if it is safe to share
- whether cookies were used
- whether the failure happens during analyze or download
- exact error text
- relevant queue diagnostics or log snippets

Avoid posting private cookies, tokens, or sensitive logs publicly.

## Best In-App Recovery Path

If a site suddenly breaks, this order usually makes sense:

1. retry once
2. update `yt-dlp`
3. add cookies if the site needs sign-in
4. review diagnostics and logs
5. file an issue with clean reproduction details

## Related Pages

- [Updates, Runtimes, and Compatibility](Updates-Runtimes-and-Compatibility.md)
- [Settings, Cookies, and YouTube Access](Settings-Cookies-and-YouTube-Access.md)
- [FAQ](FAQ.md)
