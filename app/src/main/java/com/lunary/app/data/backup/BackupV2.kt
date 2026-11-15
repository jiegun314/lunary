package com.lunary.app.data.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Streaming v2 container: magic || salt || iterations || iv || AES-GCM ciphertext.
 * The ciphertext is a zip archive streamed from a temp file, so attachments are never
 * held in memory and ZIP64 handles archives larger than 4 GB.
 */
object BackupV2 {
    const val MAGIC = "LUNARYBK2"
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun isV2(header: ByteArray): Boolean =
        header.size >= MAGIC.length &&
            header.copyOfRange(0, MAGIC.length).toString(Charsets.UTF_8) == MAGIC

    fun write(passphrase: CharArray, input: File, output: OutputStream) {
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = BackupCrypto.deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        DataOutputStream(output).use { data ->
            data.write(MAGIC.toByteArray(Charsets.UTF_8))
            data.write(salt)
            data.writeInt(BackupCrypto.ITERATIONS)
            data.write(iv)
            data.flush()
            CipherOutputStream(data, cipher).use { cipherOut ->
                input.inputStream().use { it.copyTo(cipherOut) }
            }
        }
    }

    fun readToFile(passphrase: CharArray, input: InputStream, target: File) {
        val data = DataInputStream(input)
        val magic = ByteArray(MAGIC.length)
        data.readFully(magic)
        if (magic.toString(Charsets.UTF_8) != MAGIC) error("不是 Lunary 备份文件")
        val salt = ByteArray(16).also(data::readFully)
        val iterations = data.readInt()
        require(iterations in 10_000..2_000_000) { "备份参数异常" }
        val iv = ByteArray(12).also(data::readFully)
        val key = BackupCrypto.deriveKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        try {
            CipherInputStream(data, cipher).use { cipherIn ->
                target.outputStream().use { cipherIn.copyTo(it) }
            }
        } catch (e: AEADBadTagException) {
            target.delete()
            throw AEADBadTagException("密码错误或备份文件已损坏")
        } catch (e: Exception) {
            target.delete()
            if (e is javax.crypto.AEADBadTagException) throw e
            throw AEADBadTagException("密码错误或备份文件已损坏")
        }
    }
}
