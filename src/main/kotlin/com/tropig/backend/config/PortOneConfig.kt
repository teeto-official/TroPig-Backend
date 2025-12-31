package com.tropig.backend.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(PortOneProperties::class)
class PortOneConfig {

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}