package com.lunary.app.data.backup

import java.io.File
import java.nio.file.Files
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupV2Test {
    private val password = "correct horse battery".toCharArray()

    @Test
    fun roundTripsStreamingArchive() {
        val dir = Files.createTempDirectory("backup-v2").toFile()
        val source = File(dir, "source.bin").apply {
            writeBytes(ByteArray(3 * 1024 * 1024) { (it % 251).toByte() })
        }
        val encrypted = File(dir, "backup.lunary")
        source.inputStream().use { input ->
            encrypted.outputStream().use { output ->
                BackupV2.write(password, source, output)
            }
        }
        val header = encrypted.inputStream().use { it.readNBytes(BackupV2.MAGIC.length) }
        assertTrue(BackupV2.isV2(header))

        val restored = File(dir, "restored.bin")
        encrypted.inputStream().use { input ->
            BackupV2.readToFile(password, input, restored)
        }
        assertArrayEquals(source.readBytes(), restored.readBytes())
        dir.deleteRecursively()
    }

    @Test(expected = AEADBadTagException::class)
    fun wrongPasswordFails() {
        val dir = Files.createTempDirectory("backup-v2-wrong").toFile()
        val source = File(dir, "source.bin").apply { writeBytes(ByteArray(4096)) }
        val encrypted = File(dir, "backup.lunary")
        source.inputStream().use { input ->
            encrypted.outputStream().use { output -> BackupV2.write(password, source, output) }
        }
        encrypted.inputStream().use { input ->
            BackupV2.readToFile("wrong password".toCharArray(), input, File(dir, "out.bin"))
        }
    }

    @Test(expected = AEADBadTagException::class)
    fun truncatedArchiveFails() {
        val dir = Files.createTempDirectory("backup-v2-trunc").toFile()
        val source = File(dir, "source.bin").apply { writeBytes(ByteArray(4096)) }
        val encrypted = File(dir, "backup.lunary")
        source.inputStream().use { input ->
            encrypted.outputStream().use { output -> BackupV2.write(password, source, output) }
        }
        val truncated = File(dir, "truncated.lunary")
        val bytes = encrypted.readBytes()
        truncated.writeBytes(bytes.copyOf(bytes.size - 8))
        truncated.inputStream().use { input ->
            BackupV2.readToFile(password, input, File(dir, "out.bin"))
        }
    }

    @Test
    fun headerUsesV2Magic() {
        assertEquals("LUNARYBK2", BackupV2.MAGIC)
    }
}
