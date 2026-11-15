package com.lunary.app.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Versioned authenticated encryption for backup archives:
 * magic (10 bytes) || salt (16) || iv (12) || AES-GCM ciphertext.
 */
object BackupCrypto {
    internal const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun encrypt(passphrase: CharArray, plaintext: ByteArray): ByteArray {
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plaintext)
        return BACKUP_MAGIC.toByteArray(Charsets.UTF_8) + salt + iv + encrypted
    }

    fun decrypt(passphrase: CharArray, payload: ByteArray): ByteArray {
        val magic = BACKUP_MAGIC.toByteArray(Charsets.UTF_8)
        require(payload.size > magic.size + 28) { "备份文件不完整" }
        require(payload.copyOfRange(0, magic.size).contentEquals(magic)) { "不是 Lunary 备份文件" }
        val salt = payload.copyOfRange(magic.size, magic.size + 16)
        val iv = payload.copyOfRange(magic.size + 16, magic.size + 28)
        val ciphertext = payload.copyOfRange(magic.size + 28, payload.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    internal fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
