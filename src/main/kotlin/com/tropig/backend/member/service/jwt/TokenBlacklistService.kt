package com.tropig.backend.member.service.jwt

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.TimeUnit

@Component
class TokenBlacklistService(private val redisTemplate: RedisTemplate<String, String>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "token_blacklist:"
    }

    fun blacklist(token: String, expiration: Date) {
        val remainingMs = expiration.time - System.currentTimeMillis()
        if (remainingMs <= 0) return

        try {
            val key = "$KEY_PREFIX${hash(token)}"
            redisTemplate.opsForValue().set(key, "1", remainingMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            logger.warn("토큰 블랙리스트 등록 실패 - error={}", e.message)
        }
    }

    fun isBlacklisted(token: String): Boolean = try {
        val key = "$KEY_PREFIX${hash(token)}"
        redisTemplate.hasKey(key)
    } catch (e: Exception) {
        logger.warn("토큰 블랙리스트 조회 실패 - error={}", e.message)
        false
    }

    private fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
