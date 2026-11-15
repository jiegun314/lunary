package com.lunary.app.data.backup

import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
    private val passphrase = "correct horse battery".toCharArray()
    private val plaintext = "Lunary backup content".toByteArray()

    @Test
    fun roundTrip() {
        val encrypted = BackupCrypto.encrypt(passphrase, plaintext)
        assertArrayEquals(plaintext, BackupCrypto.decrypt(passphrase, encrypted))
    }

    @Test
    fun wrongPasswordFailsAuthentication() {
        val encrypted = BackupCrypto.encrypt(passphrase, plaintext)
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt("wrong password".toCharArray(), encrypted)
        }
    }

    @Test
    fun tamperedMagicIsRejected() {
        val encrypted = BackupCrypto.encrypt(passphrase, plaintext)
        encrypted[0] = 'X'.code.toByte()
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(passphrase, encrypted)
        }
    }

    @Test
    fun encryptionUsesFreshSaltAndIv() {
        val first = BackupCrypto.encrypt(passphrase, plaintext)
        val second = BackupCrypto.encrypt(passphrase, plaintext)
        assertFalse(first.contentEquals(second))
    }
}
