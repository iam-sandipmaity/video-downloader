# Future Plan: video-downloader Roadmap

---

## What Stays the Same

- Download approach: paste URL → yt-dlp analyzes → format selection → download
- Download engine: `DownloadEngine`, `ProcessRunner`, `YtDlpExecutor` — no changes
- Backend architecture: Room DB, WorkManager, Hilt DI, DataStore — no changes
- Package name: `com.localdownloader` — maintain update compatibility
- YouTube logic: 360p hardcoded, no fallback chain (current v1.3.0 behavior)

---

## Phase 1: UI Overhaul — 3 Bottom Tabs Layout (HIGH PRIORITY)

**Design reference:** modern 3-tab Android app layout (Browser / Progress / Video)

### 1.1 Bottom Navigation Bar — 3 Tabs

Three tabs, always visible at the bottom:

| Tab | Icon | Purpose |
|---|---|---|
| **Browser** | Tab/group icon | Main page — URL input, quick site links, WebView browsing |
| **Progress** | Download icon | Active/queued download tasks with progress bars |
| **Video** | Video library icon | Completed downloads list with thumbnails, file size, play/share/delete actions |

**Technical approach (Compose):**
- `NavigationBar` + `NavigationBarItem` from Material3
- `NavHost` switching between 3 screens
- Existing `DownloadManagerScreen` becomes the **Progress** tab
- New `VideoScreen` becomes the **Video** tab (completed downloads list + internal player)
- Existing `HomeScreen` becomes **Browser** tab (heavily restructured)

### 1.2 Browser Tab (Home Screen)

This is the main landing screen. Layout top to bottom:

#### Top Bar
- App title / logo on the left
- 3-dot overflow menu on the right

#### Overflow Menu Items (dropdown)
- **History** — navigate to download history screen (overlays on video tab or separate fragment)
- **Desktop Mode** — toggle (checkbox), affects WebView user-agent
- **Settings** — navigate to existing `SettingsScreen`
- **Help** — open FAQ/info screen
- **Dark Mode** — toggle (checkbox), switches theme

These menu items will be added to existing Settings logic; some will be new screens.

#### URL / Search Bar
- OutlinedTextField: `"Search or enter URL"`
- Paste button (clipboard icon)
- Analyze button (or arrow/send icon)
- Shows progress bar while analyzing (current behavior but cleaner)

#### Quick Links Grid
- Grid of clickable site icons (2 rows of 4)
- Sites: YouTube, Instagram, Twitter/X, TikTok, Facebook, Dailymotion, Vimeo, Pinterest, Twitch
- Tapping one loads that site in the WebView below

#### Embedded WebView
- Full WebView component below the quick links
- User can browse any site in-app
- Orange download FAB button appears when downloadable video is detected
- The WebView handles navigation, back/forward, tabs (optional)

**Technical approach:**
- `WebView` component in Compose via `AndroidView`
- URL intercept to detect downloadables (reuse current `UrlValidator`)
- Quick links grid: `LazyVerticalGrid` with icon buttons
- The existing analyze/download flow kicks in when a downloadable URL is detected

### 1.3 Progress Tab

**What exists now:** `DownloadManagerScreen` + download queue
**What to improve:**
- Rounded cards with thumbnail, filename, progress bar, speed/ETA, 3-dot overflow menu
- 3-dot menu options: Cancel, Pause, Resume, Stop and Save
- Active downloads at top, queued below
- Badge/notification if background download is running

**No change to backend** — just UI styling to match the card layout shown in screenshots.

### 1.4 Video Tab (Completed Downloads)

**What it is:** NEW screen — completed download history with file management

**Layout:**
- List of downloaded files with:
  - Thumbnail
  - Filename + file size
  - 3-dot menu: Play, Share, Delete, Rename

**Play functionality:**
- Tap the file → opens internal video player (ExoPlayer or Compose MediaPlayer)
- Basic controls: play/pause, seek bar, fullscreen toggle

**Technical approach:**
- New `VideoScreen` composable
- `LazyColumn` of `VideoCard` items
- `ExoPlayer` (already available via existing dependencies or add it)
- New `PlayerScreen` composable
- Reuse existing `DownloadHistoryScreen` data where possible

### 1.5 Material Design Cleanup

**What exists now:** Current Compose theme with dark mode
**What to add:**
- Keep the dark/light toggle (add to 3-dot menu AND settings)
- Consistent elevation and surface colors across all screens
- Matching card styles (rounded corners, consistent padding)
- Bottom navigation should have proper active/inactive highlight (like super-video-downloader's pill highlight)

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
- Match YTDLnis's multi-client token model

### 2.3 Cookie Management via WebView
**What it does:**
- User logs into YouTube via embedded WebView
- Cookies extracted and saved as Netscape format
- yt-dlp uses them for authenticated downloads

### 2.4 Extractor Client Selection
**What it does:**
- Settings to pick preferred YouTube player client
- Default to `android` (least likely blocked)

### 2.5 Browser Interception Fallback (EMERGENCY ONLY)
**When to use:** yt-dlp fully breaks for YouTube
**What it does:**
- WebView loads YouTube page, intercepts `.m3u8`/`.mpd` streams
- Raw stream download (no yt-dlp merge)

---

## Implementation File Map

| New File | Tab/Feature | Description |
|---|---|---|
| `MainActivity.kt` (modify) | Navigation | Bottom navigation setup, NavHost for 3 tabs |
| `ui/BrowserScreen.kt` (new) | Browser tab | URL bar, quick links, WebView, overflow menu |
| `ui/components/QuickLinksGrid.kt` (new) | Browser tab | Site icon grid component |
| `ui/ProgressScreen.kt` (new) | Progress tab | Active download cards list (replaces DownloadManagerScreen) |
| `ui/VideoScreen.kt` (new) | Video tab | Completed downloads list |
| `ui/PlayerScreen.kt` (new) | Video tab | Internal video player (ExoPlayer) |
| `ui/components/DownloadCard.kt` (new) | Progress tab | Single download progress card UI |
| `ui/components/VideoCard.kt` (modify) | Video tab | Add file size, play/share actions |
| `ui/components/BrowserToolbar.kt` (new) | Browser tab | Top bar with 3-dot menu |
| `ui/screens/SettingsScreen.kt` (modify) | Shared | Desktop mode toggle, help section |
| `ui/screens/HistoryScreen.kt` (modify) | Browser menu item | Reuse existing download history |
| `ui/screens/HelpScreen.kt` (new) | Browser menu item | FAQ / about screen |
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

| Priority | What | Status |
|---|---|---|
| **P0** | 3 bottom tabs (Browser, Progress, Video) | Phase 1 |
| **P0** | Browser tab: URL bar + quick links + WebView | Phase 1 |
| **P0** | 3-dot overflow menu: History, Desktop Mode, Settings, Help, Dark Mode | Phase 1 |
| **P1** | Video tab: completed downloads list + internal player | Phase 1 |
| **P1** | Progress tab card redesign (match screenshot style) | Phase 1 |
| **P2** | YouTube: auto-update yt-dlp, PO tokens per client | Phase 2 |
| **P3** | Cookie management via WebView | Phase 2 |
| **P4** | Browser stream interception (emergency fallback) | Phase 2 |
