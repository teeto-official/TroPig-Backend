package com.tropig.backend.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class HealthController {
    
    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "service" to "TroPig Backend"
        )
    }
    
    @GetMapping("/")
    fun root(): Map<String, String> {
        return mapOf(
            "message" to "Welcome to TroPig Backend API",
            "version" to "1.0.0",
            "docs" to "/swagger-ui.html"
        )
    }
}
