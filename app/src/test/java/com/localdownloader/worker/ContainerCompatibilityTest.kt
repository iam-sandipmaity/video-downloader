package com.localdownloader.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContainerCompatibilityTest {
    @Test
    fun resolveCompatibleContainerFallback_prefersWebmForMp4RequestWithWebmSources() {
        val fallback = resolveCompatibleContainerFallback(
            requestedExtension = "mp4",
            videoExtension = "webm",
            audioExtension = "opus",
            stderr = "Could not find tag for codec vp9 in stream #0, codec not currently supported in container",
        )

        assertEquals("webm", fallback)
    }

    @Test
    fun resolveCompatibleContainerFallback_usesMkvForWebmCopyFailure() {
        val fallback = resolveCompatibleContainerFallback(
            requestedExtension = "webm",
            videoExtension = "webm",
            audioExtension = "opus",
            stderr = "Postprocessing:   Stream #1:0 -> #0:1 (copy)",
        )

        assertEquals("mkv", fallback)
    }

    @Test
    fun resolveCompatibleContainerFallback_returnsNullWhenNoContainerSignalExists() {
        val fallback = resolveCompatibleContainerFallback(
            requestedExtension = "webm",
            videoExtension = "webm",
            audioExtension = "opus",
            stderr = "network timeout while downloading fragments",
        )

        assertNull(fallback)
    }
}
