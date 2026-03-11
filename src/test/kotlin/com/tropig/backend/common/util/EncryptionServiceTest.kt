package com.tropig.backend.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

/**
 * EncryptionService 단위 테스트
 */
class EncryptionServiceTest {

    private fun createValidEncryptionService(): EncryptionService {
        // Generate valid 32-byte key for AES-256
        val validKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        return EncryptionService(validKey)
    }

    @Test
    fun `should encrypt and decrypt successfully`() {
        // Given
        val service = createValidEncryptionService()
        val plaintext = "1234567890"

        // When
        val encrypted = service.encrypt(plaintext)
        val decrypted = service.decrypt(encrypted)

        // Then
        assertEquals(plaintext, decrypted)
        assertNotEquals(plaintext, encrypted)
    }

    @Test
    fun `should produce different ciphertext for same plaintext`() {
        // Given
        val service = createValidEncryptionService()
        val plaintext = "test data"

        // When
        val encrypted1 = service.encrypt(plaintext)
        val encrypted2 = service.encrypt(plaintext)

        // Then
        assertNotEquals(encrypted1, encrypted2, "Each encryption should produce different ciphertext due to random IV")
        assertEquals(plaintext, service.decrypt(encrypted1))
        assertEquals(plaintext, service.decrypt(encrypted2))
    }

    @Test
    fun `should handle Korean characters correctly`() {
        // Given
        val service = createValidEncryptionService()
        val plaintext = "홍길동의 계좌번호"

        // When
        val encrypted = service.encrypt(plaintext)
        val decrypted = service.decrypt(encrypted)

        // Then
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should fail to encrypt blank string`() {
        // Given
        val service = createValidEncryptionService()

        // When & Then
        assertThrows<IllegalArgumentException> {
            service.encrypt("")
        }
    }

    @Test
    fun `should fail to decrypt blank string`() {
        // Given
        val service = createValidEncryptionService()

        // When & Then
        assertThrows<IllegalArgumentException> {
            service.decrypt("")
        }
    }

    @Test
    fun `should fail to decrypt invalid encrypted text`() {
        // Given
        val service = createValidEncryptionService()
        val invalidEncrypted = "invalid-encrypted-data"

        // When & Then
        assertThrows<IllegalArgumentException> {
            service.decrypt(invalidEncrypted)
        }
    }

    @Test
    fun `should fail to decrypt tampered ciphertext`() {
        // Given
        val service = createValidEncryptionService()
        val plaintext = "original data"
        val encrypted = service.encrypt(plaintext)

        // Tamper with the encrypted data
        val tamperedEncrypted = encrypted.substring(0, encrypted.length - 4) + "XXXX"

        // When & Then
        assertThrows<Exception> {
            service.decrypt(tamperedEncrypted)
        }
    }

    @Test
    fun `should handle long strings`() {
        // Given
        val service = createValidEncryptionService()
        val longPlaintext = "A".repeat(1000)

        // When
        val encrypted = service.encrypt(longPlaintext)
        val decrypted = service.decrypt(encrypted)

        // Then
        assertEquals(longPlaintext, decrypted)
    }

    @Test
    fun `should handle special characters`() {
        // Given
        val service = createValidEncryptionService()
        val plaintext = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`"

        // When
        val encrypted = service.encrypt(plaintext)
        val decrypted = service.decrypt(encrypted)

        // Then
        assertEquals(plaintext, decrypted)
    }
}
