# Project Audit Report: Video Downloader (Android)

## Overview
An Android video downloader app (Jetpack Compose + Hilt + Room) powered by yt-dlp and FFmpeg binaries. Features: URL analysis, download queue, video compression, format conversion, local playback, and settings management.

---

## CRITICAL ISSUES (Must Fix)

### 1. Compressor — No Validation or Input File Probing
- **`Compressor.kt`** blindly accepts `CompressionRequest` values with zero validation. No check that input file exists, no media probing to adapt bitrate/resolution to actual source.
- **Problem:** If user sets 2500kbps video bitrate for a 360p source, it will "up-compress" (larger file, no quality gain). If source is 480p but target maxHeight=1080, it still wastes bitrate.
- **Also:** The `-vf scale=-2:$maxHeight` filter will fail if `maxHeight > source` on some codecs, or produce artifacts.

### 2. FormatConverter — No Format Compatibility Checks
- **`FormatConverter.kt`** has no validation on output format vs input content. Converting an audio-only MP3 to MP4 will produce a broken/fatal output with only a black screen or silent video.
- **Problem:** User tries "converting" an MP3 to AVI and gets a confusing error with no guidance.

### 3. `Compressor.kt` and `FormatConverter.kt` share identical progress-parsing code
- The `durationPattern`, `timePattern`, `parseFfmpegDuration`, `parseFfmpegTime` are **duplicated verbatim** in both files. Single responsibility violation.

### 4. `MediaToolsViewModel.kt` has a custom `update` extension function that shadows the real one
- Line 137-139: `private fun <T> MutableStateFlow<T>.update(transform: (T) -> T)` — This is a reimplementation that does `value = transform(value)`, which is **not atomic** like the real `MutableStateFlow.update`. It can lose updates under concurrent access.

---

## HIGH PRIORITY ISSUES

### 5. CompressUI — Resolution slider without full preset labels
- **`CompressScreen.kt:138-170`**: The resolution slider maps `steps = [360, 480, 720, 1080, 1440, 2160]` but no 144p/240p option (commonly needed for sharing). Labels only shown at endpoints. User has no clear indication of what each tick means.
- **Bitrate fields are free text** with placeholder hints. Users who don't know "what is kbps" will be confused.

### 6. ConvertScreen — No guidance on which format to choose for what use case
- **`ConvertScreen.kt`**: Format dropdown just lists formats with no description of what each is good for. No "quality presets" like the Compress screen has.

### 7. Input Path is a raw text field
- Both Compress and Convert screens require users to type/see a full file path. The "Browse" button calls `FileUtils.getRealPathFromUri()` which copies the file to `cacheDir` — this is a silent, undocumented behavior. If the file is large (e.g., 2GB video), it will fill the cache and crash.

### 8. No file size display or confirmation
- After compression/conversion, the success message just says "Saved to: /path/...". No output file size shown, no before/after comparison, so user can't tell if compression actually reduced size.

---

## MEDIUM PRIORITY ISSUES

### 9. Settings Screen — Overloaded with YouTube Auth Complexity
- **`SettingsScreen.kt:251-389`**: The YouTube Auth section is very dense with nested surfaces, manual fields, bundle import, status checks. This is the most complex part of settings and would benefit from a separate sub-screen or step-by-step flow.

### 10. Settings has no compressor/converter defaults
- No way to save preferred compression preset or conversion format as a default. Every time user opens compress, they re-enter the same values.

### 11. `DownloadEngine.kt` — YouTube auth cookie check has a bug
- Line 47-49: `usesYoutubeAuthExtractor = options.extractorArgs?.contains("po_token=") == true` — cookies are only applied if `extractorArgs` contains `po_token=`. But a user could import cookies without a PO token (just cookies.txt for age-gated content). Those cookies would be silently ignored.

### 12. `ProcessRunner.kt` — FFmpeg output streams are buffered entirely
- `stdoutBuilder` and `stderrBuilder` accumulate the entire FFmpeg output in memory. For long conversion/compression jobs, this could be megabytes of text. The stderr should not be fully buffered for ffmpeg — we only need it for error reporting on failure.

### 13. No cancellation for FFmpeg operations
- `Compressor.compress()` and `FormatConverter.convert()` have no mechanism to cancel an ongoing FFmpeg process. Unlike downloads which go through WorkManager (cancellable), media operations are fire-and-forget in a coroutine scope.

### 14. `FileUtils.getRealPathFromUri()` copies to cacheDir
- The `companion object` function at line 262-275 copies content URIs to `cacheDir` with no size limit or cleanup policy. The cache directory can grow unbounded.

---

## LOW PRIORITY / CODE QUALITY

### 15. Model files missing from source tree
- `CompressionRequest.kt`, `ConversionRequest.kt`, `VideoQuality.kt`, `StreamType.kt`, `MediaToolsUiState.kt` were not findable in the project directory. They may exist but weren't picked up by the file search.

### 16. `DownloaderApp.kt:59` — `FileUtils` instantiated directly with `remember`
- `val fileUtils = remember(context) { FileUtils(context) }` — This bypasses Hilt DI. All other components get `FileUtils` via constructor injection.

### 17. No unit tests
- Only basic androidTest dependencies declared. No test source directory visible.

### 18. `CompressScreen.kt` — inconsistent Modifier import
- Line 343: `modifier = androidx.compose.ui.Modifier.padding(...)` — uses fully qualified name instead of the imported `Modifier`.

### 19. Hardcoded GitHub URLs in Settings
- Report dialog and support links are hardcoded. No way to update them without rebuilding the app.

---

## UI IMPROVEMENT PLAN (Compress / Convert / Settings)

### Compress Screen — What should change:
1. **Replace slider with a dropdown** showing labeled presets: `144p`, `240p`, `360p`, `480p`, `720p HD`, `1080p FHD`, `1440p QHD`, `2160p 4K`
2. **Bitrate presets as dropdown too**: `Small (500kbps)`, `Medium (1000kbps)`, `High (2500kbps)`, `Very High (5000kbps)`, `Auto (recommended)`
3. Manual input fields remain available below as "advanced overrides"
4. Show file picker result as a **card with filename, size, duration** instead of raw text field
5. After completion: show **before/after file size** comparison

### Convert Screen — What should change:
1. **Purpose-driven presets** instead of raw extension list: `High Quality Video (MP4)`, `Best Compatibility (MP4)`, `Small File (WebM)`, `Audio Only (MP3)`, `Lossless Audio (FLAC)`, etc.
2. Manual format selection available as "Advanced" expandable section
3. Same file picker improvement as Compress
4. Show output file size after conversion

### Settings Screen — Full Revamp:
1. **Separate pages or expandable groups** — YouTube Auth should be its own sub-screen
2. Group by: `Appearance`, `Downloads`, `Media Tools`, `YouTube Auth`, `About`
3. Add **Media Tools settings**: default compression preset, default output format, default output folder
4. Visual cards with icons for each section
5. Cleaner visual hierarchy with less nested Surface containers

---

## IMPLEMENTATION PHASING

1. **Phase 1**: Compressor logic improvements (validation, dedup progress parser, proper presets)
2. **Phase 2**: Convert logic improvements (compatibility checks, presets)
3. **Phase 3**: Compress & Convert UI rebuild (dropdowns, file cards, before/after sizes)
4. **Phase 4**: Settings page revamp
