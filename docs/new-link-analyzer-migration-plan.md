# New Link Analyzer Migration Plan

## Goal

Migrate the app from the current "analyze one URL into one `VideoInfo`" flow to a
discovery-first flow that separates:

1. link discovery
2. format loading
3. queue building

The target is to keep our current Compose UI, worker stack, typed models, and
download reliability while adopting a more flexible analysis behavior.

## What We Are Migrating

We want the following behaviors:

- source-aware link classification
- lightweight playlist and channel discovery
- incremental item loading
- deferred format loading
- refreshable per-item or multi-item format retrieval
- format-view modes such as `All`, `Suggested`, `Smallest`, and generic fallback

We do not want to directly port:

- `ResultItem`
- `DownloadItem`
- Fragment or RecyclerView UI
- text-heavy `format_note` parsing as the primary source of truth

Our migration should keep structured models and typed selection logic centered
around `MediaFormat`, `FormatChoice`, and Compose state.

## Current Flow

Current path:

- `FormatViewModel.analyzeUrlInternal`
- `DownloadRepositoryImpl.analyzeUrl`
- `FormatExtractor.analyze`
- `VideoInfo`
- `buildChoiceBundle`
- `BrowserScreen` bottom sheet

This path assumes that link discovery and format loading happen together.

## Target Flow

Target path:

1. UI submits a URL.
2. ViewModel asks repository for source analysis.
3. Repository returns a lightweight source result:
   - single item
   - playlist
   - channel feed
   - generic URL
4. UI renders discovered items immediately.
5. Formats are loaded only when needed:
   - current item
   - selected playlist items
   - manual refresh
6. Existing queue logic converts chosen formats into `DownloadOptions`.

## New Domain Models

Add these new models under `app/src/main/java/com/localdownloader/domain/models`:

- `LinkAnalysisResult.kt`
  - root response for discovery
  - contains URL metadata, source kind, title, thumbnail, item count, and items
- `LinkAnalysisItem.kt`
  - one discovered playable item
  - id, title, webpageUrl, uploader, duration, thumbnail, playlist metadata
  - optional lightweight `formats` list if discovery already includes them
- `LinkSourceKind.kt`
  - `SINGLE_VIDEO`
  - `PLAYLIST`
  - `CHANNEL`
  - `GENERIC_URL`
- `FormatLoadRequest.kt`
  - one item or many items to load formats for
- `FormatLoadResult.kt`
  - item URL/id plus resolved `List<MediaFormat>`
- `FormatViewMode.kt`
  - `ALL`
  - `SUGGESTED`
  - `SMALLEST`
  - `GENERIC`

Keep `VideoInfo.kt` during migration for download execution compatibility. Do not
delete it until queue building no longer depends on it.

## Repository Contract Changes

Update `domain/repositories/DownloaderRepository.kt` conceptually with new APIs:

- keep existing:
  - `suspend fun analyzeUrl(...)`
- add:
  - `suspend fun analyzeLink(url: String, cookiesPath: String?, userAgent: String?): Result<LinkAnalysisResult>`
  - `suspend fun loadFormats(url: String, cookiesPath: String?, userAgent: String?): Result<List<MediaFormat>>`
  - `suspend fun loadFormatsForItems(urls: List<String>, ...): Result<List<FormatLoadResult>>`

Implementation lives in:

- `app/src/main/java/com/localdownloader/data/DownloadRepositoryImpl.kt`

Migration note:

- `analyzeUrl` can temporarily delegate to `analyzeLink` plus `loadFormats` for
  single-item legacy callers.

## Downloader Layer Changes

Split `FormatExtractor.kt` responsibilities into discovery and format loading.

Recommended new files in `app/src/main/java/com/localdownloader/downloader`:

- `LinkAnalyzer.kt`
  - classifies input
  - runs lightweight source discovery
  - supports fast playlist/channel analysis
  - returns `LinkAnalysisResult`
- `FormatLoader.kt`
  - loads formats for one item
  - supports cached info-json reuse
  - supports bulk multi-item loading
  - returns typed `MediaFormat`
- `LinkSourceClassifier.kt`
  - central URL classification rules
- `LinkAnalysisMapper.kt`
  - maps yt-dlp JSON into `LinkAnalysisResult` and `LinkAnalysisItem`

Keep `FormatExtractor.kt` initially, but reduce it to a compatibility adapter:

- `analyze()` can call `LinkAnalyzer + FormatLoader` during migration
- existing queue/download callers stay stable while the new browse flow lands

## ViewModel Changes

Primary file:

- `viewmodel/FormatViewModel.kt`

Recommended state changes in:

- `app/src/main/java/com/localdownloader/viewmodel/FormatUiState.kt`

Add fields such as:

- `linkAnalysis: LinkAnalysisResult?`
- `discoveredItems: List<LinkAnalysisItemUiState>`
- `selectedBrowseItemUrl: String?`
- `isLoadingFormats: Boolean`
- `formatViewMode: FormatViewMode`
- `formatSourceSummary: String?`

Add a new UI state model:

- `DiscoveredItemUiState.kt`
  - item metadata
  - selection flags
  - expansion flags
  - loaded formats
  - available format choices
  - loading/error state

Migration behavior for `FormatViewModel`:

1. `analyzeUrl()` becomes discovery-first.
2. For single links:
   - auto-load formats after discovery.
3. For playlists/channels:
   - show discovered items immediately.
   - load formats only for:
     - selected items
     - opened item
     - explicit refresh
4. Build `FormatChoice` lists from loaded `MediaFormat`, not from raw text labels.

## UI Changes

Primary file:

- `app/src/main/java/com/localdownloader/ui/screens/BrowserScreen.kt`

Target UI structure:

- top card remains the URL entry point
- results area becomes a discovery list instead of only "analyzed one item"
- single item:
  - current options sheet can remain
- playlist/channel:
  - show discovered items first
  - allow batch selection
  - add "Load formats" and "Refresh formats"
  - allow global format mode/filter controls

Recommended new Compose components:

- `BrowseDiscoveryCard`
- `BrowseDiscoveryList`
- `BrowseItemCard`
- `FormatModeChipRow`
- `LoadFormatsActionBar`

Do not port an older bottom-sheet UI literally. Rebuild the same
behavior in Compose.

## Format View Strategy

Keep our typed format model and add view modes on top of it.

Implement in `FormatViewModel` or a helper such as:

- `viewmodel/FormatPresentationBuilder.kt`

Responsibilities:

- `ALL`
  - every valid typed format
- `SUGGESTED`
  - keep current quality-aware grouped choices
- `SMALLEST`
  - smallest option per effective quality bucket
- `GENERIC`
  - synthetic fallback choices like current "auto/mp4/mp3" style outputs

Important:

- do not drive filtering from `format_note.contains("audio")`
- use structured fields such as `vcodec`, `acodec`, `resolution`, `bitrateKbps`,
  `fileSizeBytes`, and `isVideoOnly`

## Queue Compatibility

Queue building should stay compatible during migration.

Keep these files stable as long as possible:

- `viewmodel/FormatViewModel.kt`
- `domain/models/DownloadOptions.kt`
- `worker/DownloadWorker.kt`

Bridge strategy:

- selected `LinkAnalysisItem` + loaded `MediaFormat` + chosen `FormatChoice`
  should still produce the same `DownloadOptions`
- reuse `infoJsonPath` when available for single-item downloads
- for playlist items, use per-item URL queueing with current worker behavior

## Phased Implementation

### Phase 1

- add new domain models
- add `LinkSourceClassifier`
- add `LinkAnalyzer`
- add `FormatLoader`
- keep current UI untouched
- make `FormatExtractor` delegate internally where practical

### Phase 2

- update `DownloadRepositoryImpl`
- add new repository methods
- let `FormatViewModel` run discovery-first internally
- preserve current single-video UX

### Phase 3

- extend `FormatUiState`
- add discovered item state
- update `BrowserScreen` to render discovery results for playlists/channels

### Phase 4

- add format view modes
- add lazy format loading actions
- support multi-item format refresh

### Phase 5

- remove old one-shot assumptions from `FormatViewModel`
- trim or retire legacy `VideoInfo` browse usage
- keep `VideoInfo` only if download execution still benefits from it

## Recommended First Code Slice

The safest first implementation slice is:

1. add `LinkSourceKind`, `LinkAnalysisResult`, `LinkAnalysisItem`
2. extract URL classification into `LinkSourceClassifier`
3. create `LinkAnalyzer` using the current `FormatExtractor` parsing logic
4. add `loadFormats(url)` beside current `analyze()`
5. make `FormatViewModel.analyzeUrlInternal()` branch on source kind

That gives us a real migration start without rewriting the entire browse screen
in one shot.

## Risks

- queue code currently assumes analysis and format selection happen together
- playlist item state is currently derived from `VideoInfo.playlistEntries`
- browse UI currently expects one active `videoInfo`
- a literal port from an older app would weaken model quality by relying more on raw
  text parsing and less on typed fields

## Recommendation

Proceed with a staged migration, not a rewrite.

The best end state is:

- discovery-first analysis behavior
- our current typed models and downloader reliability
- Compose-native browse and format UI
- compatibility preserved for worker/download execution throughout the rollout
