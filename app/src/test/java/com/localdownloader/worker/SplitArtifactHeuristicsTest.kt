package com.localdownloader.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SplitArtifactHeuristicsTest {
    @Test
    fun managedSplitArtifactDetection_onlyMatchesWorkerManagedNames() {
        assertTrue(isManagedSplitArtifactName("sample.video.mp4"))
        assertTrue(isManagedSplitArtifactName("sample.audio.m4a"))
        assertFalse(isManagedSplitArtifactName("sample.f248.webm"))
    }

    @Test
    fun splitArtifactRoleFromName_treatsBareWebmAsAmbiguous() {
        assertNull(splitArtifactRoleFromName(File("sample.f248.webm")))
        assertEquals(SplitArtifactRole.VIDEO, splitArtifactRoleFromName(File("sample.video.webm")))
        assertEquals(SplitArtifactRole.AUDIO, splitArtifactRoleFromName(File("sample.audio.webm")))
    }

    @Test
    fun selectDistinctSplitArtifacts_managedOnly_ignoresLooseFragments() {
        val dir = createTempDir(prefix = "split-artifacts-")
        try {
            val looseVideo = createArtifact(dir, "sample.f248.webm", 1_000L)
            val looseAudio = createArtifact(dir, "sample.f251.webm", 2_000L)
            val managedVideo = createArtifact(dir, "sample.video.mp4", 3_000L)
            val managedAudio = createArtifact(dir, "sample.audio.m4a", 4_000L)

            val selection = selectDistinctSplitArtifacts(
                candidates = listOf(looseVideo, looseAudio, managedVideo, managedAudio),
                allowLooseArtifacts = false,
                detectRole = ::splitArtifactRoleFromName,
            )

            requireNotNull(selection)
            assertEquals(managedVideo.absolutePath, selection.first.absolutePath)
            assertEquals(managedAudio.absolutePath, selection.second.absolutePath)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun selectDistinctSplitArtifacts_keepsVideoAndAudioDistinct() {
        val dir = createTempDir(prefix = "split-artifacts-")
        try {
            val looseVideo = createArtifact(dir, "sample.f248.webm", 1_000L)
            val looseAudio = createArtifact(dir, "sample.f251.webm", 2_000L)

            val selection = selectDistinctSplitArtifacts(
                candidates = listOf(looseAudio, looseVideo),
                allowLooseArtifacts = true,
                detectRole = { file ->
                    when (file.name) {
                        looseVideo.name -> SplitArtifactRole.VIDEO
                        looseAudio.name -> SplitArtifactRole.AUDIO
                        else -> null
                    }
                },
            )

            requireNotNull(selection)
            assertEquals(looseVideo.absolutePath, selection.first.absolutePath)
            assertEquals(looseAudio.absolutePath, selection.second.absolutePath)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun createArtifact(dir: File, name: String, modifiedAt: Long): File {
        return File(dir, name).apply {
            writeBytes(byteArrayOf(1, 2, 3))
            setLastModified(modifiedAt)
        }
    }
}
