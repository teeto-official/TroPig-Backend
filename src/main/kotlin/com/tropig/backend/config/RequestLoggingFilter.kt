package com.tropig.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        val uri = request.requestURI
        val query = request.queryString?.let { "?$it" } ?: ""
        val method = request.method
        val clientIp = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr

        try {
            filterChain.doFilter(request, response)
        } finally {
            val elapsed = System.currentTimeMillis() - startTime
            val status = response.status
            log.info("[API] {} {}{} | status={} | {}ms | ip={}", method, uri, query, status, elapsed, clientIp)
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return uri.startsWith("/swagger-ui") ||
            uri.startsWith("/v3/api-docs") ||
            uri.startsWith("/actuator")
    }
}
