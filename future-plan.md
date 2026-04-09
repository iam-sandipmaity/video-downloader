# Future Plan: video-downloader Roadmap

---

## What Stays the Same

- Download approach: paste URL → yt-dlp analyzes → format selection → download
- Download engine: `DownloadEngine`, `ProcessRunner`, `YtDlpExecutor` — no changes
- Backend architecture: Room DB, WorkManager, Hilt DI, DataStore — no changes
- Package name: `com.localdownloader` — maintain update compatibility
- No WebView anywhere — all download logic stays as-is

---

## Phase 1: UI Overhaul — 3 Bottom Tabs Layout (COMPLETED in v1.4.0)

### ✅ Completed Features

| Feature | Status |
|---|---|
| 3 Bottom Tabs (Browser, Progress, Video) | ✅ Complete |
| Browser Tab with URL bar + quick shortcuts | ✅ Complete |
| Progress Tab with active downloads | ✅ Complete |
| Video Tab with completed downloads + player | ✅ Complete |
| Overflow menu (History, Compressor, Converter, Settings, Help, Dark Mode) | ✅ Complete |
| Dark/Light theme toggle | ✅ Complete |

---

## Phase 2: YouTube Download Enhancement (MEDIUM PRIORITY)

### 2.1 YouTube DASH Video+Audio for Higher Resolution

**Problem:** Currently YouTube downloads without manual selection are capped at 360p. Users want higher resolutions (720p, 1080p, etc.).

**Solution:**
- Enable proper DASH format selection via `bestvideo[height<=X]+bestaudio` selector
- Add resolution picker in format selection UI (360p, 480p, 720p, 1080p, 1440p, 4K)
- Parse available DASH formats from yt-dlp analysis response
- Display available resolutions based on video's actual available formats

**Technical approach:**
- Modify `FormatViewModel` to parse height information from video formats
- Add resolution dropdown in `BrowserScreen` options sheet
- Use yt-dlp format selector: `bestvideo[height<=720]+bestaudio/best[height<=720]`

### 2.2 Auto-update yt-dlp
**What it does:**
- Check GitHub for latest yt-dlp on app start (periodically)
- Download + replace the yt-dlp binary in app data dir
- Keeps extraction working without APK updates

**Technical approach:**
- `downloader/YtDlpUpdater.kt` — new component
- GitHub API: fetches latest release, downloads updated Python binary

### 2.3 Enhanced PO Token Support
**What exists:** Has PO token + cookies path + context hint input
**What to improve:**
- Per-player-client PO tokens (android, web, ios, tv)
- Let user assign different tokens to different clients

### 2.4 Cookie Management via WebView
**What it does:**
- User logs into YouTube via a small embedded WebView (settings screen only)
- Cookies extracted and saved as Netscape format
- yt-dlp uses them for authenticated downloads

---

## Phase 3: Embedded Terminal Feature (NEW - HIGH PRIORITY)

### 3.1 Embedded Terminal for Downloads

**Problem:** Users want more control over downloads with custom yt-dlp commands.

**Solution:**
- Add embedded terminal in app where users can run yt-dlp commands directly
- Pre-filled command templates for common operations
- Real-time output streaming
- Command history

**Features:**
```
Terminal Commands:
- ytdlp <url>                           # Download with default options
- ytdlp <url> -f bestvideo[height<=1080]+bestaudio  # 1080p
- ytdlp <url> --cookies-from-browser chrome        # Use browser cookies
- ytdlp <url> -x --audio-format mp3                # Extract audio as MP3
- ytdlp -U                               # Update yt-dlp to latest
```

**Technical approach:**
- New `TerminalScreen.kt` composable
- Use same `ProcessRunner` as downloads for command execution
- Output displayed in scrollable terminal-style view
- Pre-built command templates as quick-action buttons

---

## Phase 4: Compressor & Converter Improvements (MEDIUM PRIORITY)

### 4.1 Compressor Enhancements

**Current state:** Basic compression with resolution/bitrate sliders

**Improvements needed:**
- Add compression presets (Small, Medium, Large, Custom)
- Show estimated output size before compression
- Add more resolution options (144p, 240p, 360p, 480p, 720p, 1080p)
- Add audio quality options (64kbps, 128kbps, 192kbps, 320kbps)
- Show before/after file size comparison
- Add cancel button during compression

### 4.2 Converter Enhancements

**Current state:** Basic format conversion

**Improvements needed:**
- Add format presets for common conversions
- Show estimated output size
- Support more output formats
- Batch conversion capability

---

## Phase 5: Bug Fixes & Stability (ONGOING)

### 5.1 Known Issues to Fix

| Priority | Issue | Status |
|---|---|---|
| HIGH | Cookie auth - cookies not applied without PO token | ✅ Fixed v1.4.0 |
| HIGH | Download button - confusing state during download | ✅ Fixed v1.4.0 |
| MEDIUM | Converter output not visible in file manager | ✅ Fixed v1.4.0 |
| MEDIUM | Compressor output not visible in file manager | ✅ Fixed v1.4.0 |
| MEDIUM | No FFmpeg operation cancellation | ⚠️ Partial |
| MEDIUM | Unbounded cache growth from URI import | ⚠️ Partial |
| LOW | Help page needs more information | ✅ Fixed v1.4.0 |
| LOW | Settings missing cache clearing | ✅ Fixed v1.4.0 |

---

## Phase 6: App Size Optimization (LONG-TERM)

### 6.1 Current APK Size Issues

The app includes:
- yt-dlp Python binary (~15MB)
- FFmpeg binary (~25MB)
- Python runtime (~8MB)
- Extras (~5MB)

**Total: ~50MB+ in native libraries**

### 6.2 Optimization Strategies

| Strategy | Potential Savings |
|---|---|
| Strip unused Python modules | 3-5MB |
| Use yt-dlp minimal build | 5-8MB |
| Compress FFmpeg binary | 2-3MB |
| Remove unused ABIs (keep only arm64-v8a) | 10-15MB |
| Enable R8 aggressive minification | 2-3MB |
| Use App Bundle (split APKs) | User downloads less |

**Target:** Reduce from ~50MB to ~30MB APK

---

## Phase 7: Help & Documentation (ONGOING)

### 7.1 Help Screen Improvements (v1.4.0 - COMPLETED)

Added comprehensive sections:
- ✅ Downloads: How downloads work, where files are saved
- ✅ Converter: How to convert, supported formats
- ✅ Compressor: How to compress, settings explained
- ✅ Navigation: Browser, Progress, Video tabs
- ✅ Settings: Dark mode, YouTube auth, download location
- ✅ Troubleshooting: Common issues and solutions
- ✅ About: Credits to yt-dlp and FFmpeg

### 7.2 Future Help Improvements

- Video tutorials for common tasks
- In-app tooltip explanations
- FAQ expandable sections
- Error code reference guide

---

## Priority Summary

| Priority | What | Phase | Status |
|---|---|---|---|
| **P0** | 3 bottom tabs layout | Phase 1 | ✅ Complete |
| **P0** | YouTube DASH higher resolution downloads | Phase 2 | 🔄 In Progress |
| **P1** | Embedded terminal for yt-dlp commands | Phase 3 | 📋 Planned |
| **P1** | Compressor improvements | Phase 4 | 📋 Planned |
| **P1** | Converter improvements | Phase 4 | 📋 Planned |
| **P2** | App size optimization | Phase 6 | 📋 Planned |
| **P2** | Auto-update yt-dlp | Phase 2 | 📋 Planned |
| **P3** | Cookie management via WebView | Phase 2 | 📋 Planned |

---

## Implementation File Map

### New Files to Create

| File | Phase | Description |
|---|---|---|
| `ui/screens/TerminalScreen.kt` | Phase 3 | Embedded terminal for yt-dlp commands |
| `ui/components/TerminalOutput.kt` | Phase 3 | Terminal output display component |
| `downloader/YtDlpUpdater.kt` | Phase 2 | Auto-update yt-dlp binary |
| `ui/components/CompressionPresetCard.kt` | Phase 4 | Compression preset selection |
| `ui/components/FormatPresetCard.kt` | Phase 4 | Converter format presets |

### Files to Modify

| File | Changes |
|---|---|
| `FormatViewModel.kt` | Add resolution picker, DASH format selection |
| `BrowserScreen.kt` | Add resolution dropdown in options |
| `CompressScreen.kt` | Add presets, estimated size, cancel button |
| `ConvertScreen.kt` | Add format presets, estimated size |
| `DownloaderApp.kt` | Add Terminal tab/screen navigation |

### Completed Files (v1.4.0)

- `BrowserScreen.kt` ✅
- `ProgressScreen.kt` ✅
- `VideoScreen.kt` ✅
- `PlayerScreen.kt` ✅
- `HelpScreen.kt` ✅
- `SettingsScreen.kt` ✅
- `MediaToolsViewModel.kt` ✅
- `FormatViewModel.kt` ✅
- `DownloadEngine.kt` ✅
- `FileUtils.kt` ✅