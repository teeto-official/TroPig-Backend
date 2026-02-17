package com.tropig.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry

/**
 * Spring Retry 설정
 * PortOne API 등 외부 API 호출에 재시도 로직 적용
 */
@Configuration
@EnableRetry
class RetryConfiguration
