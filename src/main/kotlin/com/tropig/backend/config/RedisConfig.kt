package com.tropig.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * Redis 설정
 * 세션 관리 및 Caffeine에 부적합한 캐시(키 조합이 많은 데이터)를 위한 보조 캐시 매니저를 제공합니다.
 * 기본 캐시 매니저는 CacheConfig의 CaffeineCacheManager입니다.
 */
@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
    @Value("\${spring.data.redis.password}") private val pass: String?,
) {

    private val logger = LoggerFactory.getLogger(RedisConfig::class.java)

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val redisConfig = RedisStandaloneConfiguration().apply {
            hostName = host
            this.port = this@RedisConfig.port
            if (!pass.isNullOrBlank()) {
                setPassword(pass)
            }
        }
        return LettuceConnectionFactory(redisConfig)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {
            this.connectionFactory = connectionFactory

            keySerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()

            valueSerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()

            afterPropertiesSet()
        }

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper())

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues()

        val cacheConfigurations = mapOf(
            "randomGenreContents" to defaultConfig.entryTtl(Duration.ofSeconds(600)),
            "randomRuleContents" to defaultConfig.entryTtl(Duration.ofSeconds(600)),
            "creatorContentsByMember" to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "creatorAllContentsByMember" to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "revenueSummaryByMember" to defaultConfig.entryTtl(Duration.ofSeconds(600)),
            "purchaseCount" to defaultConfig.entryTtl(Duration.ofSeconds(60)),
        )

        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory))
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    @Bean
    fun cacheErrorHandler(): CacheErrorHandler = object : CacheErrorHandler {
        override fun handleCacheGetError(
            exception: RuntimeException,
            cache: org.springframework.cache.Cache,
            key: Any,
        ) {
            logger.warn("Redis Cache GET 실패 - cache={}, key={}, error={}", cache.name, key, exception.message)
        }

        override fun handleCachePutError(
            exception: RuntimeException,
            cache: org.springframework.cache.Cache,
            key: Any,
            value: Any?,
        ) {
            logger.warn("Redis Cache PUT 실패 - cache={}, key={}, error={}", cache.name, key, exception.message)
        }

        override fun handleCacheEvictError(
            exception: RuntimeException,
            cache: org.springframework.cache.Cache,
            key: Any,
        ) {
            logger.warn("Redis Cache EVICT 실패 - cache={}, key={}, error={}", cache.name, key, exception.message)
        }

        override fun handleCacheClearError(exception: RuntimeException, cache: org.springframework.cache.Cache) {
            logger.warn("Redis Cache CLEAR 실패 - cache={}, error={}", cache.name, exception.message)
        }
    }
}
