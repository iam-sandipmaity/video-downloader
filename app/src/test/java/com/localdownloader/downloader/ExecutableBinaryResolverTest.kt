package com.localdownloader.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ExecutableBinaryResolverTest {
    @Test
    fun `prefers executable ffmpeg shim when both candidates exist`() {
        val tempDir = createTempDirectory("ffmpeg-runtime").toFile()
        try {
            File(tempDir, "libffmpeg.so").writeText("library")
            File(tempDir, "libffmpeg_exec.so").writeText("binary")

            val resolved = ExecutableBinaryResolver.resolveFirstExisting(
                directory = tempDir,
                candidates = listOf("libffmpeg_exec.so", "libffmpeg.so"),
            )

            assertEquals(File(tempDir, "libffmpeg_exec.so").absolutePath, resolved?.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `returns null when no candidates exist`() {
        val tempDir = createTempDirectory("ffmpeg-runtime-empty").toFile()
        try {
            val resolved = ExecutableBinaryResolver.resolveFirstExisting(
                directory = tempDir,
                candidates = listOf("libffmpeg_exec.so", "libffmpeg.so"),
            )

            assertNull(resolved)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
