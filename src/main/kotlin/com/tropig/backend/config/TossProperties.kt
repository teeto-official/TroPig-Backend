package com.tropig.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "toss")
data class TossProperties(
    val secretKey: String = "",
    val baseUrl: String = "https://api.tosspayments.com",
    val clientKey: String = "",
)
