package com.tropig.backend.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CaffeineCacheManager {
        val cacheManager = object : CaffeineCacheManager() {

            override fun createCaffeineCache(name: String): CaffeineCache {
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

                    else -> Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)    // 디폴트 TTL
                }

                return CaffeineCache(name, caffeine.build())
            }
        }

        cacheManager.setCacheNames(
            listOf("pickContent", "pickContentByType", "nicknameComponent", "tagList")
        )

        return cacheManager
    }
}
