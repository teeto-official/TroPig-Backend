package com.tropig.backend.common.util

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 암호화 서비스
 * 민감한 데이터(계좌번호 등)를 암호화/복호화
 */
@Component
class EncryptionService(
    @Value("\${encryption.secret-key}") private val secretKeyString: String
) {
    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)

    private val algorithm = "AES/GCM/NoPadding"
    private val gcmTagLength = 128
    private val ivLength = 12
    private val requiredKeyLength = 32  // 32 bytes for AES-256

    private val secretKey: SecretKey by lazy {
        val keyBytes = try {
            Base64.getDecoder().decode(secretKeyString)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(
                "Invalid encryption key format. Key must be Base64 encoded. " +
                "Generate with: openssl rand -base64 32",
                e
            )
        }

        require(keyBytes.size == requiredKeyLength) {
            "Encryption key must be exactly $requiredKeyLength bytes for AES-256. " +
            "Current key size: ${keyBytes.size} bytes. " +
            "Generate correct key with: openssl rand -base64 32"
        }

        SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Validate encryption configuration on startup
     */
    @PostConstruct
    fun validateConfiguration() {
        try {
            // Force lazy initialization to validate key early
            val _ = secretKey
            logger.info("Encryption service initialized successfully with AES-256-GCM")

            // Test encryption/decryption
            val testData = "test"
            val encrypted = encrypt(testData)
            val decrypted = decrypt(encrypted)
            require(decrypted == testData) {
                "Encryption test failed: decrypted data does not match original"
            }
            logger.info("Encryption service validation completed")
        } catch (e: Exception) {
            logger.error("Failed to initialize encryption service", e)
            throw IllegalStateException("Encryption service initialization failed. Check encryption.secret-key configuration.", e)
        }
    }

    /**
     * 평문 암호화
     * Format: base64(iv + ciphertext + tag)
     */
    fun encrypt(plaintext: String): String {
        require(plaintext.isNotBlank()) { "Plaintext cannot be blank" }

        val cipher = Cipher.getInstance(algorithm)
        val iv = ByteArray(ivLength)
        SecureRandom().nextBytes(iv)

        val parameterSpec = GCMParameterSpec(gcmTagLength, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * 암호문 복호화
     */
    fun decrypt(encryptedText: String): String {
        require(encryptedText.isNotBlank()) { "Encrypted text cannot be blank" }

        val combined = try {
            Base64.getDecoder().decode(encryptedText)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid encrypted text format", e)
        }

        require(combined.size > ivLength) {
            "Invalid encrypted data: too short"
        }

        val iv = combined.sliceArray(0 until ivLength)
        val ciphertext = combined.sliceArray(ivLength until combined.size)

        val cipher = Cipher.getInstance(algorithm)
        val parameterSpec = GCMParameterSpec(gcmTagLength, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}
