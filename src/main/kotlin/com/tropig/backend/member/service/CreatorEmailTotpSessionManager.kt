package com.tropig.backend.member.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tropig.backend.member.entity.MemberAuthInfo
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class CreatorEmailTotpSessionManager(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        const val SESSION_TTL_MINUTES = 10L
        const val RESEND_COOLDOWN_SECONDS = 60L
        const val MAX_ATTEMPTS = 5
        private const val SESSION_PREFIX = "creator_email_totp:session:"
        private const val COOLDOWN_PREFIX = "creator_email_totp:cooldown:"
        private val random = SecureRandom()
    }

    fun createSession(memberId: Long, email: String): CreatorEmailTotpSession {
        val code = generateCode()
        val now = LocalDateTime.now()
        val session = CreatorEmailTotpSession(
            verificationId = generateVerificationId(),
            memberId = memberId,
            email = email,
            codeHash = MemberAuthInfo.sha256(code),
            expiresAt = now.plusMinutes(SESSION_TTL_MINUTES),
            retryAvailableAt = now.plusSeconds(RESEND_COOLDOWN_SECONDS),
        )

        saveSession(session)
        redisTemplate.opsForValue().set(
            cooldownKey(memberId),
            session.verificationId,
            RESEND_COOLDOWN_SECONDS,
            TimeUnit.SECONDS,
        )

        return session.copy(code = code)
    }

    fun getSession(verificationId: String): CreatorEmailTotpSession? {
        val value = redisTemplate.opsForValue().get(sessionKey(verificationId)) ?: return null
        return try {
            objectMapper.readValue(value, CreatorEmailTotpSession::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun hasCooldown(memberId: Long): Boolean = redisTemplate.hasKey(cooldownKey(memberId)) == true

    fun incrementAttempts(verificationId: String): Int {
        val session = getSession(verificationId) ?: throw SessionNotFoundException(verificationId)
        val newAttempts = session.attempts + 1

        if (newAttempts >= MAX_ATTEMPTS) {
            deleteSession(verificationId)
            throw OtpAttemptsExceededException()
        }

        saveSession(session.copy(attempts = newAttempts))
        return newAttempts
    }

    fun getRemainingAttempts(session: CreatorEmailTotpSession): Int = MAX_ATTEMPTS - session.attempts

    fun deleteSession(verificationId: String) {
        redisTemplate.delete(sessionKey(verificationId))
    }

    fun clearCooldown(memberId: Long) {
        redisTemplate.delete(cooldownKey(memberId))
    }

    fun isExpired(session: CreatorEmailTotpSession): Boolean = LocalDateTime.now().isAfter(session.expiresAt)

    private fun saveSession(session: CreatorEmailTotpSession) {
        val value = objectMapper.writeValueAsString(session.copy(code = null))
        redisTemplate.opsForValue().set(
            sessionKey(session.verificationId),
            value,
            SESSION_TTL_MINUTES,
            TimeUnit.MINUTES,
        )
    }

    private fun sessionKey(verificationId: String): String = "$SESSION_PREFIX$verificationId"

    private fun cooldownKey(memberId: Long): String = "$COOLDOWN_PREFIX$memberId"

    private fun generateVerificationId(): String = "CREATOR_EMAIL_TOTP_${UUID.randomUUID()}"

    private fun generateCode(): String = "%06d".format(random.nextInt(1_000_000))
}

data class CreatorEmailTotpSession(
    val verificationId: String,
    val memberId: Long,
    val email: String,
    val codeHash: String,
    val expiresAt: LocalDateTime,
    val retryAvailableAt: LocalDateTime,
    val attempts: Int = 0,
    val code: String? = null,
)
