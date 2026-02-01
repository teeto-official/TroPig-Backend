package com.tropig.backend.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    private val logger = LoggerFactory.getLogger(CacheConfig::class.java)

    @Bean
    fun cacheManager(): CaffeineCacheManager {
        val cacheManager = object : CaffeineCacheManager() {

            override fun createCaffeineCache(name: String): CaffeineCache {
                logger.debug("Creating cache: '$name'")
                val caffeine = when (name) {

                    // ---- 원하는 cacheName에 따라 TTL/Size 지정 ----
                    "pickContent" -> Caffeine.newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)   // TTL 3분
                        .maximumSize(40)

                    "pickContentByType" -> Caffeine.newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)    // TTL 3분
                        .maximumSize(20)

                    "nicknameComponent" -> Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)   // TTL 30분 (닉네임 리스트는 자주 변경되지 않음)

                    // 크리에이터 수익/작품 조회 관련 캐시
                    "creatorContentsByMember" -> Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)   // TTL 30분 (작품 리스트)
                        .maximumSize(5_000)

                    "revenueSummaryByMember" -> Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)   // TTL 10분 (전체 수익/출금 금액)
                        .maximumSize(10_000)


                    else -> {
                        // 디버깅: 예상하지 못한 캐시 이름이 들어오는 경우 로그 출력
                        logger.warn("Unknown cache name: '$name', using default settings")
                        Caffeine.newBuilder()
                            .expireAfterWrite(5, TimeUnit.MINUTES)    // 디폴트 TTL
                            .maximumSize(100)                           // 디폴트 크기
                    }
                }

                logger.debug("Cache '$name' created successfully")
                return CaffeineCache(name, caffeine.build())
            }
        }

        // createCaffeineCache를 오버라이드하면 자동으로 동적 캐시 생성이 지원됩니다.
        // setCacheNames를 호출하지 않으면, @Cacheable이 사용될 때 자동으로 createCaffeineCache가 호출됩니다.

        return cacheManager
    }
}
