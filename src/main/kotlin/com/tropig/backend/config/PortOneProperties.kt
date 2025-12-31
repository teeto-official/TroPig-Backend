package com.tropig.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "portone")
data class PortOneProperties(
    val secretKey: String?,
    val baseUrl: String = "https://api.portone.io"
)

