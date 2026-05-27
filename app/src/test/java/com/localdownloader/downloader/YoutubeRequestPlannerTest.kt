package com.localdownloader.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeRequestPlannerTest {

    @Test
    fun recoveryCandidates_excludesSelectedExtractorArgs() {
        val selected = "youtube:player_client=default,android"

        val candidates = YoutubeRequestPlanner.recoveryCandidates(
            selectedExtractorArgs = selected,
            cookiesAvailable = true,
        )

        assertFalse(candidates.contains(selected))
        assertTrue(candidates.contains(null))
        assertTrue(candidates.any { it?.contains("default,mweb") == true })
    }

    @Test
    fun authenticatedSameSelectorAttempts_keepRequestedSelector() {
        val attempts = YoutubeRequestPlanner.authenticatedSameSelectorAttempts(
            requestedSelector = "137+140",
            poToken = "token123",
            preferredHint = "web.gvs",
        )

        assertTrue(attempts.isNotEmpty())
        assertTrue(attempts.all { it.selector == "137+140" })
        assertTrue(attempts.first().extractorArgs?.contains("default,web") == true)
    }

    @Test
    fun buildAdaptiveSelector_preservesRequestedHeightAndContainer() {
        val selector = YoutubeRequestPlanner.buildAdaptiveSelector(
            maxHeight = 1080,
            container = "mp4",
            videoOnly = false,
        )

        assertEquals(
            "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
            selector,
        )
    }

    @Test
    fun preferredAuthenticatedExtractorArgs_preservesPreformattedPoTokensAndDataSyncId() {
        val extractorArgs = YoutubeRequestPlanner.preferredAuthenticatedExtractorArgs(
            poToken = "web.gvs+gvs123,web.player+player456,web.subs+subs789",
            preferredHint = "web.gvs",
            dataSyncId = "SYNC_ID_123",
        )

        assertEquals(
            "youtube:player_client=default,web;player_skip=webpage,configs;data_sync_id=SYNC_ID_123;po_token=web.gvs+gvs123,web.player+player456,web.subs+subs789",
            extractorArgs,
        )
    }

    @Test
    fun analyzeCandidates_keepsReleaseFallbacksAheadOfPreferredAuthenticatedArgs() {
        val preferred = "youtube:player_client=default,web;player_skip=webpage,configs;po_token=web.gvs+token123"

        val candidates = YoutubeRequestPlanner.analyzeCandidates(
            intent = YoutubeAnalyzeIntent.EXPLICIT_VIDEO,
            cookiesAvailable = true,
            preferredExtractorArgs = preferred,
        )

        assertTrue(candidates.isNotEmpty())
        assertEquals(null, candidates.first())
        assertTrue(candidates[1]?.contains("default,android") == true)
        assertEquals(preferred, candidates.last())
        assertTrue(candidates.indexOf(null) < candidates.indexOf(preferred))
    }

    @Test
    fun analyzeCandidates_restores1723ReleaseOrderForSingleVideoFallbacks() {
        val candidates = YoutubeRequestPlanner.analyzeCandidates(
            intent = YoutubeAnalyzeIntent.EXPLICIT_VIDEO,
            cookiesAvailable = false,
        )

        assertEquals(null, candidates.first())
        assertTrue(candidates[1]?.contains("default,android") == true)
        assertTrue(candidates[2]?.contains("tv,android") == true)
        assertFalse(candidates[1]?.contains("default,mweb") == true)
        assertFalse(candidates[1]?.contains("default,web") == true)
        assertTrue(candidates[1]?.contains("player_skip=webpage,configs") == true)
    }

    @Test
    fun analyzeCandidates_usesLightweightListFallbacksForPlaylistStyleUrls() {
        val preferred = "youtube:player_client=default,web;player_skip=webpage,configs;po_token=web.gvs+token123"

        val candidates = YoutubeRequestPlanner.analyzeCandidates(
            intent = YoutubeAnalyzeIntent.PLAYLIST,
            cookiesAvailable = true,
            preferredExtractorArgs = preferred,
        )

        assertEquals(null, candidates.first())
        assertTrue(candidates[1]?.contains("default,mweb,web,android,tv") == true)
        assertTrue(candidates[2]?.contains("default,web") == true)
        assertEquals(preferred, candidates.last())
        assertFalse(candidates.any { it?.contains("player_client=default,android;") == true })
    }
}
