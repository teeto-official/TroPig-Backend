package com.tropig.backend.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
                        .expireAfterWrite(3, TimeUnit.MINUTES) // TTL 3분
                        .maximumSize(40)

                    "pickContentByType" -> Caffeine.newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES) // TTL 3분
                        .maximumSize(20)

                    // 비로그인(GUEST) 및 장르/룰 미설정 회원 랜덤 컨텐츠 캐시 (type+isAdult 기준)
                    "randomContents" -> Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(10)

                    // 장르 기반 랜덤 컨텐츠 캐시 (type+isAdult+genres 기준)
                    "randomGenreContents" -> Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)

                    // 룰 기반 랜덤 컨텐츠 캐시 (isAdult+rules 기준)
                    "randomRuleContents" -> Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)

                    "nicknameComponent" -> Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES) // TTL 30분 (닉네임 리스트는 자주 변경되지 않음)

                    // 크리에이터 발행된 작품 조회 캐시
                    "creatorContentsByMember" -> Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES) // TTL 30분 (발행된 작품은 자주 변경되지 않음)
                        .maximumSize(5_000)

                    // 크리에이터 임시저장 작품 조회 캐시
                    "getDraftContent" -> Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES) // TTL 5분 (임시저장은 자주 수정됨)
                        .maximumSize(5_000)

                    "revenueSummaryByMember" -> Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES) // TTL 10분 (전체 수익/출금 금액)
                        .maximumSize(10_000)

                    else -> {
                        // 디버깅: 예상하지 못한 캐시 이름이 들어오는 경우 로그 출력
                        logger.warn("Unknown cache name: '$name', using default settings")
                        Caffeine.newBuilder()
                            .expireAfterWrite(5, TimeUnit.MINUTES) // 디폴트 TTL
                            .maximumSize(100) // 디폴트 크기
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
