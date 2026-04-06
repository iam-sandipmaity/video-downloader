# Future Plan: video-downloader Roadmap

---

## What Stays the Same

- Download approach: paste URL → yt-dlp analyzes → format selection → download
- Download engine: `DownloadEngine`, `ProcessRunner`, `YtDlpExecutor` — no changes
- Backend architecture: Room DB, WorkManager, Hilt DI, DataStore — no changes
- Package name: `com.localdownloader` — maintain update compatibility
- YouTube logic: 360p hardcoded, no fallback chain (current v1.3.0 behavior)
- No WebView anywhere — all download logic stays as-is

---

## Phase 1: UI Overhaul — 3 Bottom Tabs Layout (HIGH PRIORITY)

**Design reference:** 3-tab Android app layout (Browser / Progress / Video)

### 1.1 Bottom Navigation Bar — 3 Tabs

Three tabs, always visible at the bottom:

| Tab | Icon | Purpose |
|---|---|---|
| **Browser** | Tab/group icon | Main page — URL input bar, quick site shortcut buttons |
| **Progress** | Download icon | Active/queued download tasks with progress bars |
| **Video** | Video library icon | Completed downloads list with thumbnails, file size, play/share/delete |

**Technical approach (Compose):**
- `NavigationBar` + `NavigationBarItem` from Material3
- `NavHost` switching between 3 screens
- Existing `DownloadManagerScreen` becomes the **Progress** tab
- New `VideoScreen` becomes the **Video** tab (completed downloads + internal player)
- Existing `HomeScreen` becomes **Browser** tab (renamed, restructured layout)

### 1.2 Browser Tab (Home Screen)

This is the main landing screen. The download logic is exactly what we already have
(URL input → analyze → format select → download) — just laid out differently.

#### Top Bar
- App title / logo on the left
- 3-dot overflow menu on the right

#### Overflow Menu Items (dropdown)
- **History** — opens `DownloadHistoryScreen`
- **Compressor** — opens `CompressScreen` (already exists)
- **Converter** — opens `ConvertScreen` (already exists)
- **Settings** — opens existing `SettingsScreen`
- **Help** — opens FAQ/info screen
- **Dark Mode** — toggle (checkbox), switches theme

These are all existing screens that currently live elsewhere in the app — the overflow
menu just acts as a unified navigation hub.

#### URL / Search Bar
- OutlinedTextField: `"Search or enter URL"`
- Paste button (clipboard icon)
- Analyze button
- Linear progress bar shown while analyzing

#### Quick Shortcut Buttons
- Horizontal scrollable row of site chips/pills (YouTube, Instagram, TikTok, Twitter/X,
  Facebook, Dailymotion, Vimeo, Pinterest, Twitch)
- Tapping one fills the URL bar with that site's domain or opens YouTube search
  (tapping "YouTube" → `https://youtube.com`, tapping "TikTok" → `https://tiktok.com`, etc.)
- User then pastes or types the full URL — existing analyze flow kicks in

**Technical approach:**
- `LazyRow` of chip buttons or icon grid
- No WebView — no in-app browsing
- The existing `HomeScreen` URL input + analyze logic stays as-is
- Just reorganized into a cleaner layout matching the 3-tab pattern

### 1.3 Progress Tab

**What exists now:** `DownloadManagerScreen` + download queue
**What to improve:**
- Rounded cards with thumbnail, filename, progress bar, speed/ETA, 3-dot overflow menu
- 3-dot menu options: Cancel, Pause, Resume, Stop and Save
- Active downloads at top, queued below
- Badge count on tab icon when background downloads are running

**No change to backend** — just new `DownloadCard` component with the card layout.

### 1.4 Video Tab (Completed Downloads)

**What it is:** NEW screen — completed download history with file management

**Layout:**
- `LazyColumn` of cards with:
  - Thumbnail
  - Filename + file size
  - 3-dot menu: Play, Share, Delete, Rename

**Play functionality:**
- Tap the file → opens internal video player (ExoPlayer)
- Basic controls: play/pause, seek bar, fullscreen toggle

**Technical approach:**
- New `VideoScreen` composable
- New `PlayerScreen` composable with ExoPlayer
- Reuse existing `DownloadHistoryScreen` data where possible

### 1.5 Material Design Cleanup

**What exists now:** Current Compose theme with dark mode
**What to add:**
- Dark/light toggle in overflow menu AND settings
- Consistent elevation and surface colors across all screens
- Rounded card style everywhere (progress cards, video cards)
- Bottom navigation with proper active/inactive highlight (filled/tonal distinction)

---

## Phase 2: YouTube Download Logic Enhancement (MEDIUM PRIORITY — Long-term)

### 2.1 Auto-update yt-dlp
**What it does:**
- Check GitHub for latest yt-dlp on app start (periodically)
- Download + replace the yt-dlp binary in app data dir
- Keeps extraction working without APK updates

**Technical approach:**
- `downloader/YtDlpUpdater.kt` — new component
- GitHub API: fetches latest release, downloads updated Python binary

### 2.2 Enhanced PO Token Support
**What exists:** Has PO token + cookies path + context hint input
**What to improve:**
- Per-player-client PO tokens (android, web, ios, tv)
- Let user assign different tokens to different clients

### 2.3 Cookie Management via WebView
**What it does:**
- User logs into YouTube via a small embedded WebView (settings screen only)
- Cookies extracted and saved as Netscape format
- yt-dlp uses them for authenticated downloads

### 2.4 Extractor Client Selection
**What it does:**
- Settings to pick preferred YouTube player client
- Default to `android` (least likely blocked)

---

## Implementation File Map

| New File | Tab/Feature | Description |
|---|---|---|
| `MainActivity.kt` (modify) | Navigation | Bottom navigation setup, NavHost for 3 tabs |
| `ui/BrowserScreen.kt` (new) | Browser tab | URL bar, quick site shortcuts, overflow menu |
| `ui/components/QuickLinksRow.kt` (new) | Browser tab | Horizontal site shortcut chips |
| `ui/ProgressScreen.kt` (new) | Progress tab | Active download cards list (replaces DownloadManagerScreen) |
| `ui/VideoScreen.kt` (new) | Video tab | Completed downloads list |
| `ui/PlayerScreen.kt` (new) | Video tab | Internal video player (ExoPlayer) |
| `ui/components/DownloadCard.kt` (new) | Progress tab | Single download progress card UI |
| `ui/components/VideoCard.kt` (modify) | Video tab | Add file size, play/share actions |
| `ui/components/BrowserToolbar.kt` (new) | Browser tab | Top BarLayout with 3-dot overflow menu |
| `ui/screens/SettingsScreen.kt` (modify) | Shared | Compressor/Converter moved to overflow menu, help section |
| `ui/screens/HelpScreen.kt` (new) | Overflow menu | FAQ / about screen |
| `downloader/YtDlpUpdater.kt` (new) | Phase 2 | Auto-update yt-dlp binary |

### Existing Files to Keep (Backend — No Changes)
- `DownloadEngine.kt`, `ProcessRunner.kt`, `YtDlpExecutor.kt`
- `DownloadWorker.kt` (WorkManager)
- `DownloadRepositoryImpl.kt`, `DownloadTaskStore.kt`
- Room: `DownloadTaskEntity`, `DownloadTaskDao`, `AppDatabase`
- `BinaryInstaller.kt`, `FormatExtractor.kt`, `FormatViewModel.kt`
- All domain models

### Existing Files to Delete/Replace (Old UI Only)
- `ui/screens/HomeScreen.kt` — replaced by `BrowserScreen.kt`
- `ui/screens/DownloadManagerScreen.kt` — replaced by `ProgressScreen.kt`
- `ui/DownloaderApp.kt` — modify nav structure
- `ui/screens/DownloadHistoryScreen.kt` — merge into `VideoScreen.kt`

---

## Priority Summary

| Priority | What | Phase |
|---|---|---|
| **P0** | 3 bottom tabs (Browser, Progress, Video) | Phase 1 |
| **P0** | Browser tab: URL bar + quick site shortcuts + 3-dot menu | Phase 1 |
| **P0** | Overflow menu: History, Compressor, Converter, Settings, Help, Dark Mode | Phase 1 |
| **P1** | Video tab: completed downloads list + internal player | Phase 1 |
| **P1** | Progress tab card redesign (rounded cards with 3-dot menu) | Phase 1 |
| **P2** | YouTube: auto-update yt-dlp, PO tokens per client | Phase 2 |
| **P3** | Cookie management via WebView (settings only) | Phase 2 |
