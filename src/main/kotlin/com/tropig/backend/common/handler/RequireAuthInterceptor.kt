package com.tropig.backend.common.handler

import com.tropig.backend.common.annotation.RequireAuth
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RequireAuthInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {

        if (handler !is HandlerMethod) return true

        val requireAuth =
            handler.getMethodAnnotation(RequireAuth::class.java) != null ||
                    handler.beanType.getAnnotation(RequireAuth::class.java) != null

        if (!requireAuth) return true

        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return false
        }

        return true
    }
}