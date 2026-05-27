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
    fun analyzeCandidates_prioritizesPreferredAuthenticatedArgsFirst() {
        val preferred = "youtube:player_client=default,web;player_skip=webpage,configs;po_token=web.gvs+token123"

        val candidates = YoutubeRequestPlanner.analyzeCandidates(
            cookiesAvailable = true,
            preferredExtractorArgs = preferred,
        )

        assertTrue(candidates.isNotEmpty())
        assertEquals(preferred, candidates.first())
    }

    @Test
    fun analyzeCandidates_prefersLightweightMwebCandidateBeforeDefaultFallback() {
        val candidates = YoutubeRequestPlanner.analyzeCandidates(cookiesAvailable = false)

        assertTrue(candidates.first()?.contains("default,mweb") == true)
        assertTrue(candidates.first()?.contains("player_skip=webpage,configs") == true)
        assertTrue(candidates.contains(null))
    }
}
