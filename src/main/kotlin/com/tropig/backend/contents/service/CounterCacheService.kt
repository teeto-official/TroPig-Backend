package com.tropig.backend.contents.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class CounterCacheService(private val redisTemplate: RedisTemplate<String, String>) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BOOKMARK_PREFIX = "bookmark_count:"
        private const val FAVORITE_PREFIX = "favorite_count:"
        private const val TTL_MINUTES = 30L
    }

    fun getBookmarkCount(contentId: Long): Long? = getCount("$BOOKMARK_PREFIX$contentId")

    fun setBookmarkCount(contentId: Long, count: Long) = setCount("$BOOKMARK_PREFIX$contentId", count)

    fun incrementBookmark(contentId: Long) = increment("$BOOKMARK_PREFIX$contentId")

    fun decrementBookmark(contentId: Long) = decrement("$BOOKMARK_PREFIX$contentId")

    fun getFavoriteCount(contentId: Long): Long? = getCount("$FAVORITE_PREFIX$contentId")

    fun setFavoriteCount(contentId: Long, count: Long) = setCount("$FAVORITE_PREFIX$contentId", count)

    fun incrementFavorite(contentId: Long) = increment("$FAVORITE_PREFIX$contentId")

    fun decrementFavorite(contentId: Long) = decrement("$FAVORITE_PREFIX$contentId")

    private fun getCount(key: String): Long? = try {
        redisTemplate.opsForValue().get(key)?.toLongOrNull()
    } catch (e: Exception) {
        logger.warn("카운터 캐시 조회 실패 - key={}, error={}", key, e.message)
        null
    }

    private fun setCount(key: String, count: Long) {
        try {
            redisTemplate.opsForValue().set(key, count.toString(), TTL_MINUTES, TimeUnit.MINUTES)
        } catch (e: Exception) {
            logger.warn("카운터 캐시 저장 실패 - key={}, error={}", key, e.message)
        }
    }

    private fun increment(key: String) {
        try {
            if (redisTemplate.hasKey(key)) {
                val result = redisTemplate.opsForValue().increment(key) ?: return
                if (result == 1L) {
                    redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES)
                }
            }
        } catch (e: Exception) {
            logger.warn("카운터 증가 실패 - key={}, error={}", key, e.message)
        }
    }

    private fun decrement(key: String) {
        try {
            if (redisTemplate.hasKey(key)) {
                val result = redisTemplate.opsForValue().decrement(key) ?: return
                if (result < 0) {
                    redisTemplate.opsForValue().set(key, "0", TTL_MINUTES, TimeUnit.MINUTES)
                }
            }
        } catch (e: Exception) {
            logger.warn("카운터 감소 실패 - key={}, error={}", key, e.message)
        }
    }
}
