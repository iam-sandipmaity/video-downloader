# Quick Download Share Sheet Overlay Specification

## 1. Overview & Vision

The **Quick Download Share Sheet Overlay** is a high-efficiency, floating modal interface designed for downloading media instantly when a URL is shared from third-party apps (such as video streaming apps, social media, or browsers) into Video Downloader.

The core design philosophy is **instant visual recognition, zero-friction choice, and 1-tap execution**:
- **Immediate Clarity**: Users instantly see what video was shared through rich media artwork, duration, and title.
- **Zero-Menu Friction**: Audio and Video options are displayed side-by-side in direct, categorized visual cards rather than buried under nested dropdown menus.
- **Adaptive 1-Tap Action**: Tapping any quality tile selects it and updates the primary dynamic CTA button (`Download <Quality> • <Size>`), enabling one-handed instant queueing.
- **Non-Intrusive Power Options**: Multi-threading fragment steppers and format container overrides are tucked into a clean expandable quick-tuning bar.

---

## 2. Visual Layout & UI Architecture

The interface uses a **Modal Bottom Sheet** paradigm with standard 28dp top corner rounding, smooth entry/exit animations, and an interactive backdrop scrim.

### 2.1. Single Video Overlay Layout

```
┌────────────────────────────────────────────────────────┐
│                      (Backdrop Scrim)                  │
│                                                        │
│ ┌────────────────────────────────────────────────────┐ │
│ │                  ─ Drag Handle ─                   │ │
│ │                                                    │ │
│ │  ┌─────────┐  Video Title (Editable with ✏️)       │ │
│ │  │Thumb-   │  Channel Name • youtube.com       [✕] │ │
│ │  │nail     │                                       │ │
│ │  │ [03:45] │                                       │ │
│ │  └─────────┘                                       │ │
│ │ ────────────────────────────────────────────────── │ │
│ │                                                    │ │
│ │  🎵 MUSIC / AUDIO                                  │ │
│ │  ┌───────────────────────┐ ┌─────────────────────┐ │ │
│ │  │ MP3 · 320k     [HQ]   │ │ MP3 · 128k          │ │ │
│ │  │ 8.4 MB                │ │ 3.2 MB              │ │ │
│ │  └───────────────────────┘ └─────────────────────┘ │ │
│ │  ┌───────────────────────┐ ┌─────────────────────┐ │ │
│ │  │ M4A · 256k            │ │ FLAC · Lossless     │ │ │
│ │  │ 6.1 MB                │ │ 24.5 MB             │ │ │
│ │  └───────────────────────┘ └─────────────────────┘ │ │
│ │                                                    │ │
│ │  🎬 VIDEO                                          │ │
│ │  ┌───────────────────────┐ ┌─────────────────────┐ │ │
│ │  │ 1080p FHD   [60fps] 🔘│ │ 720p HD     [BEST]  │ │ │
│ │  │ MP4 · 48.2 MB         │ │ MP4 · 22.1 MB       │ │ │
│ │  └───────────────────────┘ └─────────────────────┘ │ │
│ │  ┌───────────────────────┐ ┌─────────────────────┐ │ │
│ │  │ 480p SD               │ │ 360p                │ │ │
│ │  │ MP4 · 12.4 MB         │ │ MP4 · 7.8 MB        │ │ │
│ │  └───────────────────────┘ └─────────────────────┘ │ │
│ │                                                    │ │
│ │  ⚙️ Quick Settings (Threads: 4 ▾ | Container: MP4)  │ │
│ │                                                    │ │
│ │  ┌───────────────────────────────────────────────┐ │ │
│ │  │      ⬇️  Download 1080p (48.2 MB)             │ │ │
│ │  └───────────────────────────────────────────────┘ │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

### 2.2. Playlist Batch Download Overlay Layout

When a playlist URL is detected and parsed, the overlay presents an interactive selection list of all videos alongside unified target quality options:

```
┌────────────────────────────────────────────────────────┐
│                      (Backdrop Scrim)                  │
│                                                        │
│ ┌────────────────────────────────────────────────────┐ │
│ │                  ─ Drag Handle ─                   │ │
│ │                                                    │ │
│ │  ┌─────────┐  [PLAYLIST]                           │ │
│ │  │Thumb-   │  Ultimate Synthwave Mix 2026          │ │
│ │  │nail     │  SynthChannel • youtube.com       [✕] │ │
│ │  │ [24 vid]│                                       │ │
│ │  └─────────┘                                       │ │
│ │ ────────────────────────────────────────────────── │ │
│ │                                                    │ │
│ │  📋 Playlist Items (24/24 selected)   [Deselect All]│ │
│ │  ┌───────────────────────────────────────────────┐ │ │
│ │  │ ☑ #1 [Thumb] 04:12  Neon Sunset Drive        │ │ │
│ │  │ ☑ #2 [Thumb] 03:45  Cyberpunk Highway        │ │ │
│ │  │ ☑ #3 [Thumb] 05:20  Midnight City Lights     │ │ │
│ │  └───────────────────────────────────────────────┘ │ │
│ │                                                    │ │
│ │  🎵 MUSIC / AUDIO (Applied to selected)            │ │
│ │  ┌───────────────────────┐ ┌─────────────────────┐ │ │
│ │  │ MP3 · 320k     [HQ]   │ │ MP3 · 128k          │ │ │
│ │  └───────────────────────┘ └─────────────────────┘ │ │
│ │                                                    │ │
│ │  🎬 VIDEO (Applied to selected)                    │ │
│ │  ┌───────────────────────┐ ┌─────────────────────┐ │ │
│ │  │ 1080p FHD   [60fps] 🔘│ │ 720p HD     [BEST]  │ │ │
│ │  └───────────────────────┘ └─────────────────────┘ │ │
│ │                                                    │ │
│ │  ⚙️ Quick Settings (Threads: 4 ▾ | Container: MP4)  │ │
│ │                                                    │ │
│ │  ┌───────────────────────────────────────────────┐ │ │
│ │  │    ⬇️  Download all 24 items · 1080p          │ │ │
│ │  └───────────────────────────────────────────────┘ │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 3. UI Component Breakdown

### 3.1. Rich Media Header Card (`QuickMediaHeaderCard`)
- **Thumbnail Image**: High-resolution 16:9 or square cropped image with subtle rounded corners (`12.dp`).
- **Duration / Item Count Badge**: Translucent black pill overlay displaying video duration (`03:45`) or total video count (`24 videos`) for playlists situated at the bottom-right corner of the thumbnail.
- **Playlist Badge**: Vibrant `PLAYLIST` pill badge displayed prominently above the title when a playlist URL is detected.
- **Media Title**: Two-line clamped typography with ellipsis. Includes an inline edit button (`✏️`) to quickly customize output filenames or folder names without opening extra screens.
- **Source & Author Tag**: Displays uploader name and platform source domain chip (e.g. `youtube.com`, `instagram.com`).
- **Close Button**: Accessible `[✕]` icon at top-right for quick dismissal.

---

### 3.2. Immediate Categorized Grid (`QuickMediaOptionGrid`)
Options are separated into two distinct, high-contrast sections:

#### A. 🎵 Audio / Music Section
A 2-column or 3-column grid containing all available audio quality profiles:
- **MP3 320k** (HQ badge, High Bitrate)
- **MP3 128k** (Standard)
- **M4A / AAC** (Efficient stream)
- **FLAC / WAV** (Lossless stream)
- **Opus** (Original audio)

#### B. 🎬 Video Section
A structured 2-column grid containing all available video stream choices:
- **4K UHD / 2K QHD** (Ultra High Definition)
- **1080p FHD** (with `60fps` pill when applicable)
- **720p HD** (Recommended badge)
- **480p / 360p** (Data Saver)

#### C. Option Card anatomy (`QuickOptionTile`)
Each card is an interactive surface containing:
- **Primary Label**: Bold resolution or audio format name (`1080p`, `MP3 320k`).
- **Metadata Pills**: Container format (`MP4`, `WEBM`, `M4A`) and performance badges (`60fps`, `HQ`, `RECOMMENDED`).
- **File Size Highlight**: Exact or estimated download file size (e.g., `~48.2 MB`).
- **Selection State**: Highlighted accent border (`2.dp`), tinted surface background, and an active checkmark/radio indicator.

---

### 3.3. Playlist Multi-Selection Section (`QuickPlaylistItemsSection`)
When a playlist link is shared:
- **Section Toolbar**:
  - Title with dynamic selected count chip: `Playlist Items (X / Y selected)`.
  - Batch action toggle: `Select all` / `Deselect all` 1-tap text button.
- **Playlist Item Row (`QuickPlaylistItemRow`)**:
  - Selection Checkbox: Interactive toggle state for each video.
  - Index Badge: Monospaced numerical index (`#1`, `#2`, ...).
  - Item Thumbnail: Compact cropped thumbnail with duration timestamp overlay (`04:12`).
  - Item Title & Uploader: Clamped title text with channel/author subtitle.
  - Row Click Handling: Tapping anywhere on an item row immediately toggles its checkbox.
- **Scrollable Constrained Container**: Vertically scrollable area capped at `220.dp` within the modal sheet to ensure quality options and the bottom action bar remain readily reachable.

---

### 3.4. Quick Tuning Expander (`QuickTuningBar`)
Provides granular download controls without cluttering the primary format choice:
- **Download Threads Stepper**: `[-] 4 [+]` allowing configuration from 1 to 16 concurrent fragments.
- **Container Format Override**: Quick selector chip for container packaging (`Auto`, `MP4`, `MKV`, `WebM`).
- **Subtitle Toggle** *(Optional)*: Direct checkbox to embed subtitles when available.

---

### 3.5. Dynamic Sticky CTA (`QuickDownloadActionBar`)
- Positioned permanently at the bottom of the sheet above system navigation insets.
- Dynamic label reflecting single-item or playlist batch status and target quality:
  - Single media: `⬇️ Download 1080p (48.2 MB)` or `⬇️ Download MP3 320k (8.4 MB)`
  - Playlist (all selected): `⬇️ Download all 24 items · 1080p`
  - Playlist (subset selected): `⬇️ Download 18 items · 1080p`
  - Playlist (0 items selected): `Select items to download` (disabled)
- Displays a smooth indeterminate progress indicator during queue dispatch before auto-closing with a brief confirmation toast.

---

### 3.6. Skeleton Loading State (`QuickDownloadSkeletonLoader`)
While URL metadata is analyzed via extractor runtime:
- Displays animated shimmer placeholders for the thumbnail, title lines, and grid cards.
- Prevents UI layout shifting when data finishes loading.
- Includes a polite cancel action in case the network request is slow or stalled.

---

## 4. State & Data Contracts

```kotlin
data class QuickPlaylistItem(
    val entry: PlaylistEntry,
    val isSelected: Boolean = true,
)

data class QuickDownloadUiState(
    val url: String = "",
    val isAnalyzing: Boolean = true,
    val isQueueing: Boolean = false,
    val isDownloadSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showMeteredNetworkDialog: Boolean = false,
    val showTitleEditDialog: Boolean = false,
    val isQuickSettingsExpanded: Boolean = false,
    val videoInfo: VideoInfo? = null,
    val playlistItems: List<QuickPlaylistItem> = emptyList(),
    
    // Media Header Metadata
    val title: String = "",
    val uploader: String? = null,
    val durationFormatted: String? = null,
    val thumbnailUrl: String? = null,
    val domainHost: String? = null,
    val selectedStreamType: StreamType = StreamType.VIDEO_AUDIO,
    
    // Categorized Direct Options
    val videoQualityOptions: List<QuickQualityOption> = emptyList(),
    val audioQualityOptions: List<QuickQualityOption> = emptyList(),
    val selectedVideoQuality: QuickQualityOption? = null,
    val selectedAudioQuality: QuickQualityOption? = null,
    val videoFormatOptions: List<QuickFormatOption> = emptyList(),
    val audioFormatOptions: List<QuickFormatOption> = emptyList(),
    val selectedVideoFormat: QuickFormatOption? = null,
    val selectedAudioFormat: QuickFormatOption? = null,
    
    // Advanced Quick Settings
    val threads: Int = 4,
    val appSettings: AppSettings = AppSettings(),
) {
    val isPlaylist: Boolean get() = (videoInfo?.isPlaylist == true) || playlistItems.isNotEmpty()
    val selectedPlaylistItemCount: Int get() = playlistItems.count { it.isSelected }
    val totalPlaylistItemCount: Int get() = playlistItems.size
    val areAllPlaylistItemsSelected: Boolean get() = playlistItems.isNotEmpty() && playlistItems.all { it.isSelected }
    val canDownload: Boolean get() = !isQueueing && (!isPlaylist || totalPlaylistItemCount == 0 || selectedPlaylistItemCount > 0)
}
```

---

## 5. Implementation Roadmap

### Step 1: ViewModel & Data Model Expansion
- Add rich metadata parsing (`durationFormatted`, `thumbnailUrl`, `uploader`, `domainHost`) to `QuickDownloadViewModel`.
- Consolidate video and audio format builders to produce `QuickDownloadChoice` collections directly populated with size estimations, FPS badges, and container tags.

### Step 2: Bottom Sheet Container & Header
- Migrate `QuickDownloadScreen` to a native Material 3 `ModalBottomSheet` or anchored surface card.
- Build `QuickMediaHeaderCard` with AsyncImage loading, duration badge, and title edit modal.

### Step 3: Categorized Option Grid
- Implement `QuickMediaOptionGrid` and `QuickOptionTile` with 2-column layout and animated selection state styling.
- Ensure audio and video sections are distinct, responsive, and scrollable on small screens.

### Step 4: Quick Tuning Bar & Sticky CTA
- Implement the collapsible `QuickTuningBar` for thread count and container overrides.
- Implement the sticky dynamic CTA button with loading indicator and queue dispatch logic.

### Step 5: Skeleton Loader & Error Handling
- Add shimmer skeleton layout for the analysis phase.
- Polish metered network prompts and playlist handling hints.
