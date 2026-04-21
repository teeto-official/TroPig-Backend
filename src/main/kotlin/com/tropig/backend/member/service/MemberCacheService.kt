package com.tropig.backend.member.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tropig.backend.common.model.AuthMember
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class MemberCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "member:"
        private const val TTL_MINUTES = 30L
    }

    fun getCachedMember(memberId: Long): AuthMember? {
        val value = redisTemplate.opsForValue().get("$KEY_PREFIX$memberId") ?: return null
        return try {
            objectMapper.readValue(value, AuthMember::class.java)
        } catch (e: Exception) {
            logger.warn("회원 캐시 역직렬화 실패 - memberId={}, error={}", memberId, e.message)
            null
        }
    }

    fun cacheMember(memberId: Long, authMember: AuthMember) {
        try {
            val value = objectMapper.writeValueAsString(authMember)
            redisTemplate.opsForValue().set("$KEY_PREFIX$memberId", value, TTL_MINUTES, TimeUnit.MINUTES)
        } catch (e: Exception) {
            logger.warn("회원 캐시 저장 실패 - memberId={}, error={}", memberId, e.message)
        }
    }

    fun evictMember(memberId: Long) {
        try {
            redisTemplate.delete("$KEY_PREFIX$memberId")
        } catch (e: Exception) {
            logger.warn("회원 캐시 삭제 실패 - memberId={}, error={}", memberId, e.message)
        }
    }
}
