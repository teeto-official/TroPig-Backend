package com.tropig.backend.common.util

import org.springframework.beans.factory.annotation.Value
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtil {
    @Value("\${crypto.secret-key}")
    private val secretKey: String = ""

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12          // GCM 권장 12바이트
    private const val TAG_SIZE_BITS = 128         // 인증 태그 128비트

    private val rng = SecureRandom()
    private val b64 = Base64.getEncoder()
    private val b64d = Base64.getDecoder()

    private fun keyFromBase64(): SecretKey {
        val keyBytes = Base64.getDecoder().decode(secretKey)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String, aad: ByteArray? = null): String {
        val iv = ByteArray(IV_SIZE_BYTES).also { rng.nextBytes(it) }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keyFromBase64(), spec)
        if (aad != null) cipher.updateAAD(aad)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return "${b64.encodeToString(iv)}:${b64.encodeToString(cipherBytes)}"
    }

    fun decrypt(token: String, aad: ByteArray? = null): String {
        val parts = token.split(":")
        require(parts.size == 2) { "Invalid token format. Expected 'iv:ciphertext'." }

        val iv = b64d.decode(parts[0])
        val cipherBytes = b64d.decode(parts[1])

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, keyFromBase64(), spec)
        if (aad != null) cipher.updateAAD(aad)

        val plainBytes = cipher.doFinal(cipherBytes) // aad/키/데이터 틀리면 AEADBadTagException
        return plainBytes.toString(Charsets.UTF_8)
    }
}