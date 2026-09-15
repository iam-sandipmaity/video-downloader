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
}
