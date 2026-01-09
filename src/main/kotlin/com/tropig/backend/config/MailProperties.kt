package com.tropig.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mail")
data class MailProperties(
    val sender: From?,
    val subjects: Map<String, String> = emptyMap()
) {
    data class From(
        val address: String,
        val name: String
    )
}