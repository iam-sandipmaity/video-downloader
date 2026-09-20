package com.localdownloader.viewmodel

import com.localdownloader.domain.models.FormatSelectorStyle
import com.localdownloader.domain.models.StreamType
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
}
