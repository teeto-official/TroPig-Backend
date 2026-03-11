package com.tropig.backend.member.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 본인인증 세션 관리자
 * Redis를 사용하여 임시 인증 세션을 관리합니다.
 */
@Component
class VerificationSessionManager(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val SESSION_PREFIX = "identity_verify:"
        private const val SESSION_TTL_MINUTES = 3L // OTP 유효 시간: 3분
        private const val MAX_OTP_ATTEMPTS = 5
        private const val MAX_RESEND_ATTEMPTS = 3
    }

    /**
     * 새로운 인증 세션을 생성합니다.
     */
    fun createSession(memberId: Long, portoneId: String): VerificationSession {
        val sessionId = generateSessionId()
        val session = VerificationSession(
            sessionId = sessionId,
            memberId = memberId,
            portoneId = portoneId,
            expiresAt = LocalDateTime.now().plusMinutes(SESSION_TTL_MINUTES),
            otpAttempts = 0,
            resendCount = 0,
        )

        val key = getKey(sessionId)
        val value = objectMapper.writeValueAsString(session)
        redisTemplate.opsForValue().set(key, value, SESSION_TTL_MINUTES, TimeUnit.MINUTES)

        return session
    }

    /**
     * 세션을 조회합니다.
     */
    fun getSession(sessionId: String): VerificationSession? {
        val key = getKey(sessionId)
        val value = redisTemplate.opsForValue().get(key) ?: return null

        return try {
            objectMapper.readValue(value, VerificationSession::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * OTP 시도 횟수를 증가시킵니다.
     */
    fun incrementOtpAttempts(sessionId: String): Int {
        val session = getSession(sessionId) ?: throw SessionNotFoundException(sessionId)
        val newAttempts = session.otpAttempts + 1

        if (newAttempts >= MAX_OTP_ATTEMPTS) {
            deleteSession(sessionId)
            throw OtpAttemptsExceededException()
        }

        val updatedSession = session.copy(otpAttempts = newAttempts)
        saveSession(updatedSession)
        return newAttempts
    }

    /**
     * 재전송 횟수를 증가시킵니다.
     */
    fun incrementResendCount(sessionId: String): Int {
        val session = getSession(sessionId) ?: throw SessionNotFoundException(sessionId)
        val newCount = session.resendCount + 1

        if (newCount > MAX_RESEND_ATTEMPTS) {
            throw ResendLimitExceededException()
        }

        // 재전송 시 만료 시간 갱신
        val updatedSession = session.copy(
            resendCount = newCount,
            expiresAt = LocalDateTime.now().plusMinutes(SESSION_TTL_MINUTES),
        )
        saveSession(updatedSession)

        // TTL도 갱신
        val key = getKey(sessionId)
        redisTemplate.expire(key, SESSION_TTL_MINUTES, TimeUnit.MINUTES)

        return newCount
    }

    /**
     * 세션을 삭제합니다.
     */
    fun deleteSession(sessionId: String) {
        val key = getKey(sessionId)
        redisTemplate.delete(key)
    }

    /**
     * 세션이 만료되었는지 확인합니다.
     */
    fun isExpired(session: VerificationSession): Boolean = LocalDateTime.now().isAfter(session.expiresAt)

    /**
     * 남은 재전송 횟수를 반환합니다.
     */
    fun getRemainingResends(session: VerificationSession): Int = MAX_RESEND_ATTEMPTS - session.resendCount

    /**
     * 남은 OTP 시도 횟수를 반환합니다.
     */
    fun getRemainingAttempts(session: VerificationSession): Int = MAX_OTP_ATTEMPTS - session.otpAttempts

    private fun saveSession(session: VerificationSession) {
        val key = getKey(session.sessionId)
        val value = objectMapper.writeValueAsString(session)
        redisTemplate.opsForValue().set(key, value, SESSION_TTL_MINUTES, TimeUnit.MINUTES)
    }

    private fun getKey(sessionId: String): String = "$SESSION_PREFIX$sessionId"

    private fun generateSessionId(): String = "IDENTITY_VERIFY_${UUID.randomUUID()}"
}

/**
 * 인증 세션 데이터
 */
data class VerificationSession(
    val sessionId: String,
    val memberId: Long,
    val portoneId: String,
    val expiresAt: LocalDateTime,
    val otpAttempts: Int = 0,
    val resendCount: Int = 0,
)

/**
 * 세션 관련 예외
 */
class SessionNotFoundException(sessionId: String) : RuntimeException("Verification session not found: $sessionId")

class OtpAttemptsExceededException : RuntimeException("OTP attempts exceeded (max 5 attempts)")

class ResendLimitExceededException : RuntimeException("Resend limit exceeded (max 3 resends)")
