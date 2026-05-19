package com.localdownloader.ui.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceSiteVisualsTest {

    @Test
    fun sourceSiteVisualForUrl_matchesYoutubeHosts() {
        val visual = sourceSiteVisualForUrl("https://www.youtube.com/watch?v=123")

        requireNotNull(visual)
        assertEquals("YouTube", visual.label)
        assertEquals("file:///android_asset/platform_logos/youtube.svg", visual.assetPath)
    }

    @Test
    fun sourceSiteVisualForUrl_matchesTwitterAndXHosts() {
        val visual = sourceSiteVisualForUrl("https://twitter.com/user/status/1")

        requireNotNull(visual)
        assertEquals("X", visual.label)
    }

    @Test
    fun sourceSiteVisualForUrl_returnsNullForUnknownHosts() {
        assertNull(sourceSiteVisualForUrl("https://example.com/video"))
    }

    @Test
    fun sourceHostLabel_removesWwwPrefix() {
        assertEquals("youtube.com", sourceHostLabel("https://www.youtube.com/watch?v=1"))
    }
}
