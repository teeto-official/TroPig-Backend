package com.tropig.backend.common

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RequestMapping("/api")
@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): Map<String, Any> = mapOf(
        "status" to "UP",
        "timestamp" to LocalDateTime.now(),
        "service" to "TroPig Backend",
    )

    @GetMapping("/")
    fun root(): Map<String, String> = mapOf(
        "message" to "Welcome to TroPig Backend API",
        "version" to "1.0.0",
        "docs" to "/swagger-ui.html",
    )
}
