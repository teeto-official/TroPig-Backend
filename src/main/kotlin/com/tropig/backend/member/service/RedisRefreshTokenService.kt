package com.tropig.backend.member.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisRefreshTokenService(private val redisTemplate: RedisTemplate<String, String>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "refresh_token:"
        private const val TTL_HOURS = 24L
    }

    fun save(memberId: Long, token: String) {
        try {
            redisTemplate.opsForValue().set("$KEY_PREFIX$memberId", token, TTL_HOURS, TimeUnit.HOURS)
        } catch (e: Exception) {
            logger.warn("Refresh token 저장 실패 - memberId={}, error={}", memberId, e.message)
        }
    }

    fun findByMemberId(memberId: Long): String? = try {
        redisTemplate.opsForValue().get("$KEY_PREFIX$memberId")
    } catch (e: Exception) {
        logger.warn("Refresh token 조회 실패 - memberId={}, error={}", memberId, e.message)
        null
    }

    fun deleteByMemberId(memberId: Long) {
        try {
            redisTemplate.delete("$KEY_PREFIX$memberId")
        } catch (e: Exception) {
            logger.warn("Refresh token 삭제 실패 - memberId={}, error={}", memberId, e.message)
        }
    }
}
