package com.localdownloader.viewmodel

import com.localdownloader.domain.models.FormatSelectorStyle
import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickDownloadViewModelTest {

    @Test
    fun defaultUiState_hasCorrectInitialValues() {
        val state = QuickDownloadUiState()
        assertEquals(StreamType.VIDEO_AUDIO, state.selectedStreamType)
        assertEquals(4, state.threads)
        assertEquals(FormatSelectorStyle.BOTTOM_SHEET, state.appSettings.formatSelectorStyle)
    }

    @Test
    fun quickQualityOption_displayLabelIncludesSubtitleWhenPresent() {
        val optionWithSub = QuickQualityOption(
            id = "1080p",
            title = "1080p",
            subtitle = "25.4 MB",
            height = 1080,
        )
        assertEquals("1080p · 25.4 MB", optionWithSub.displayLabel)

        val optionWithoutSub = QuickQualityOption(
            id = "720p",
            title = "720p",
            subtitle = null,
            height = 720,
        )
        assertEquals("720p", optionWithoutSub.displayLabel)
    }

    @Test
    fun ctaButtonLabel_formatsProperlyForVideoAndAudio() {
        val videoOption = QuickQualityOption(
            id = "1080p",
            title = "1080p",
            subtitle = "~45.2 MB",
            height = 1080,
        )
        val audioOption = QuickQualityOption(
            id = "320k",
            title = "MP3 320k",
            subtitle = "~8.4 MB",
            bitrateKbps = 320,
        )

        val videoState = QuickDownloadUiState(
            selectedStreamType = StreamType.VIDEO_AUDIO,
            selectedVideoQuality = videoOption,
        )
        assertEquals("Download 1080p (~45.2 MB)", videoState.ctaButtonLabel)

        val audioState = QuickDownloadUiState(
            selectedStreamType = StreamType.AUDIO_ONLY,
            selectedAudioQuality = audioOption,
        )
        assertEquals("Download MP3 320k (~8.4 MB)", audioState.ctaButtonLabel)
    }

    @Test
    fun playlistUiState_computesCountsAndCtaLabelCorrectly() {
        val entry1 = com.localdownloader.domain.models.PlaylistEntry(
            playlistItemIndex = 1,
            id = "id1",
            title = "Video 1",
            webpageUrl = "https://youtube.com/watch?v=1",
        )
        val entry2 = com.localdownloader.domain.models.PlaylistEntry(
            playlistItemIndex = 2,
            id = "id2",
            title = "Video 2",
            webpageUrl = "https://youtube.com/watch?v=2",
        )
        val quality = QuickQualityOption(
            id = "1080p",
            title = "1080p",
            height = 1080,
        )

        val items = listOf(
            QuickPlaylistItem(entry = entry1, isSelected = true),
            QuickPlaylistItem(entry = entry2, isSelected = true),
        )

        val allSelectedState = QuickDownloadUiState(
            playlistItems = items,
            selectedVideoQuality = quality,
        )

        assertEquals(true, allSelectedState.isPlaylist)
        assertEquals(2, allSelectedState.totalPlaylistItemCount)
        assertEquals(2, allSelectedState.selectedPlaylistItemCount)
        assertEquals(true, allSelectedState.areAllPlaylistItemsSelected)
        assertEquals(true, allSelectedState.canDownload)
        assertEquals("Download all 2 items · 1080p", allSelectedState.ctaButtonLabel)

        // Partially selected
        val partiallySelectedState = allSelectedState.copy(
            playlistItems = listOf(
                items[0].copy(isSelected = true),
                items[1].copy(isSelected = false),
            ),
        )
        assertEquals(1, partiallySelectedState.selectedPlaylistItemCount)
        assertEquals(false, partiallySelectedState.areAllPlaylistItemsSelected)
        assertEquals(true, partiallySelectedState.canDownload)
        assertEquals("Download 1 items · 1080p", partiallySelectedState.ctaButtonLabel)

        // None selected
        val noneSelectedState = allSelectedState.copy(
            playlistItems = listOf(
                items[0].copy(isSelected = false),
                items[1].copy(isSelected = false),
            ),
        )
        assertEquals(0, noneSelectedState.selectedPlaylistItemCount)
        assertEquals(false, noneSelectedState.areAllPlaylistItemsSelected)
        assertEquals(false, noneSelectedState.canDownload)
        assertEquals("Select items to download", noneSelectedState.ctaButtonLabel)
    }

    @Test
    fun isLikelyHlsFormat_identifiesHlsFormatsCorrectly() {
        val hlsFormat232 = com.localdownloader.domain.models.MediaFormat(
            formatId = "232",
            extension = "mp4",
            container = "mp4",
            resolution = "720p",
            videoCodec = "avc1.4d401f",
            audioCodec = "mp4a.40.2",
            fileSizeBytes = null,
            bitrateKbps = 1500,
            fps = 30.0,
            note = "m3u8",
        )
        val dashFormat136 = com.localdownloader.domain.models.MediaFormat(
            formatId = "136",
            extension = "mp4",
            container = "mp4",
            resolution = "720p",
            videoCodec = "avc1.4d401f",
            audioCodec = "none",
            fileSizeBytes = 25000000L,
            bitrateKbps = 1200,
            fps = 30.0,
            note = "DASH video",
        )

        assertEquals(true, QuickDownloadViewModel.isLikelyHlsFormat(hlsFormat232))
        assertEquals(false, QuickDownloadViewModel.isLikelyHlsFormat(dashFormat136))
    }

    @Test
    fun videoQualityEnum_hasExpectedHighResolutionEntries() {
        val qualities = com.localdownloader.domain.models.VideoQuality.entries
        assertEquals(4320, qualities.first { it.name == "FUHD_8K" }.maxHeight)
        assertEquals("8K · 4320p", qualities.first { it.name == "FUHD_8K" }.label)
        assertEquals(2160, qualities.first { it.name == "UHD_4K" }.maxHeight)
        assertEquals("4K · 2160p", qualities.first { it.name == "UHD_4K" }.label)
        assertEquals(1800, qualities.first { it.name == "QHD_3K" }.maxHeight)
        assertEquals("3K · 1800p", qualities.first { it.name == "QHD_3K" }.label)
        assertEquals(1440, qualities.first { it.name == "QHD_2K" }.maxHeight)
        assertEquals("2K · 1440p", qualities.first { it.name == "QHD_2K" }.label)
    }

    @Test
    fun buildVideoQualities_retainsHighResolutionsWhenDefaultFormatIsH264Mp4() {
        val format8k = MediaFormat(
            formatId = "571",
            extension = "webm",
            container = "webm",
            resolution = "7680x4320",
            videoCodec = "av01.0.17M.08",
            audioCodec = "none",
            fileSizeBytes = 500_000_000L,
            bitrateKbps = 35000,
            fps = 60.0,
            note = "8K VP9/AV1",
        )
        val format4k = MediaFormat(
            formatId = "313",
            extension = "webm",
            container = "webm",
            resolution = "3840x2160",
            videoCodec = "vp9",
            audioCodec = "none",
            fileSizeBytes = 200_000_000L,
            bitrateKbps = 15000,
            fps = 60.0,
            note = "4K VP9",
        )
        val format1440p = MediaFormat(
            formatId = "271",
            extension = "webm",
            container = "webm",
            resolution = "2560x1440",
            videoCodec = "vp9",
            audioCodec = "none",
            fileSizeBytes = 100_000_000L,
            bitrateKbps = 8000,
            fps = 60.0,
            note = "1440p VP9",
        )
        val format1080p = MediaFormat(
            formatId = "137",
            extension = "mp4",
            container = "mp4",
            resolution = "1920x1080",
            videoCodec = "avc1.640028",
            audioCodec = "none",
            fileSizeBytes = 50_000_000L,
            bitrateKbps = 4000,
            fps = 30.0,
            note = "1080p H264",
        )
        val format360p = MediaFormat(
            formatId = "18",
            extension = "mp4",
            container = "mp4",
            resolution = "640x360",
            videoCodec = "avc1.42001E",
            audioCodec = "mp4a.40.2",
            fileSizeBytes = 15_000_000L,
            bitrateKbps = 600,
            fps = 30.0,
            note = "360p H264",
        )
        val audioFormat = MediaFormat(
            formatId = "140",
            extension = "m4a",
            container = "m4a",
            resolution = null,
            videoCodec = "none",
            audioCodec = "mp4a.40.2",
            fileSizeBytes = 5_000_000L,
            bitrateKbps = 128,
            fps = null,
            note = "m4a audio",
        )

        val videoInfo = VideoInfo(
            id = "test_8k_video",
            title = "Test 8K Video",
            uploader = "Test Creator",
            durationSeconds = 120L,
            thumbnailUrl = null,
            webpageUrl = "https://youtube.com/watch?v=test8k",
            formats = listOf(format8k, format4k, format1440p, format1080p, format360p, audioFormat),
            extractorArgs = null,
            infoJsonPath = null,
            isPlaylist = false,
            playlistCount = null,
        )

        val selectedFormatOption = QuickFormatOption(
            id = "mp4_h264",
            label = "MP4 · H.264",
            container = "mp4",
            videoCodec = "H264",
            isOriginalStream = false,
        )

        val qualities = QuickDownloadViewModel.buildVideoQualities(videoInfo, selectedFormatOption)
        val heights = qualities.mapNotNull { it.height }

        // All resolutions must be present, not just 360p/1080p
        assertEquals(listOf(4320, 2160, 1440, 1080, 360), heights)
        assertEquals("8K · 4320p 60fps", qualities.first { it.height == 4320 }.title)
        assertEquals("4K · 2160p 60fps", qualities.first { it.height == 2160 }.title)
        assertEquals("2K · 1440p 60fps", qualities.first { it.height == 1440 }.title)
        assertEquals("1080p", qualities.first { it.height == 1080 }.title)
        assertEquals("360p", qualities.first { it.height == 360 }.title)

        // 1080p and 360p should prefer the matching H264 stream
        assertEquals("137", qualities.first { it.height == 1080 }.mediaFormat?.formatId)
        assertEquals("18", qualities.first { it.height == 360 }.mediaFormat?.formatId)

        // 8K, 4K, 1440p should smoothly fallback to the available high-res stream (av01 / vp9)
        assertEquals("571", qualities.first { it.height == 4320 }.mediaFormat?.formatId)
        assertEquals("313", qualities.first { it.height == 2160 }.mediaFormat?.formatId)
        assertEquals("271", qualities.first { it.height == 1440 }.mediaFormat?.formatId)
    }
}
