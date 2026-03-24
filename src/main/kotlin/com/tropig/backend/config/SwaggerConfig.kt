package com.tropig.backend.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    `in` = SecuritySchemeIn.HEADER,
)
class SwaggerConfig(private val environment: Environment) {

    @Bean
    fun openApi(): OpenAPI {
        val activeProfiles = environment.activeProfiles
        val serverUrl = if (activeProfiles.contains("local")) {
            "http://localhost:8080"
        } else if (activeProfiles.contains("production")) {
            "https://api.triquest.me"
        } else {
            "https://api-dev.triquest.me"
        }

        return OpenAPI()
            .addServersItem(Server().url(serverUrl))
    }
}
