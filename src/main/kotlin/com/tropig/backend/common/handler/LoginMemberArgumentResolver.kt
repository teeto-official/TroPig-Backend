package com.tropig.backend.common.handler

import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException

@Component
class LoginMemberArgumentResolver: HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(LoginMember::class.java)
                && parameter.parameterType == AuthMember::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): AuthMember? {
        val request = webRequest.nativeRequest as HttpServletRequest

        //error 같은 에러 디스패치에서는 불필요한 인증 처리로 2차 에러가 날 수 있어 방어
        if (request.requestURI == "/error") return null

        // @RequireAuth가 메서드/클래스에 붙었는지 확인
        val requireAuth =
            parameter.method?.isAnnotationPresent(RequireAuth::class.java) == true ||
                    parameter.containingClass.isAnnotationPresent(RequireAuth::class.java)

        // JwtAuthenticationFilter가 세팅한 principal(AuthMember)를 꺼낸다
        val principal = SecurityContextHolder.getContext().authentication?.principal
        val authMember = principal as? AuthMember

        // 규칙 1: @RequireAuth + @LoginMember => 무조건 인증 필요
        if (requireAuth && authMember == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.")
        }

        // 규칙 2: @RequireAuth 없음 + @LoginMember
        // - 토큰이 있으면 AuthMember 반환
        // - 토큰이 없으면 null 반환
        return authMember
    }

}