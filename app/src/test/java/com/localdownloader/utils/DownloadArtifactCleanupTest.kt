package com.localdownloader.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadArtifactCleanupTest {

    @Test
    fun matchesManagedArtifactForTemplate_acceptsExactAndDottedArtifacts() {
        assertTrue(matchesManagedArtifactForTemplate("video.%(ext)s", "video.mp4"))
        assertTrue(matchesManagedArtifactForTemplate("video.%(ext)s", "video.f248.webm"))
        assertTrue(matchesManagedArtifactForTemplate("video.%(ext)s", "video.info.json"))
        assertTrue(matchesManagedArtifactForTemplate("clip.mp4", "clip.mp4.part"))
    }

    @Test
    fun matchesManagedArtifactForTemplate_rejectsLookalikeSiblingNames() {
        assertFalse(matchesManagedArtifactForTemplate("video.%(ext)s", "video 2.mp4"))
        assertFalse(matchesManagedArtifactForTemplate("video.%(ext)s", "video-notes.txt"))
        assertFalse(matchesManagedArtifactForTemplate("video.%(ext)s", "myvideo.mp4"))
        assertFalse(matchesManagedArtifactForTemplate("clip.mp4", "clip.mp4 backup"))
    }
}
