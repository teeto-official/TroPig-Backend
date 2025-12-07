package com.tropig.backend.common.handler

import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.member.service.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException

@Component
class LoginMemberArgumentResolver(
    private val jwtTokenProvider: JwtTokenProvider,
): HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(LoginMember::class.java)
                && parameter.parameterType == Member::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): AuthMember? {
        val request = webRequest.nativeRequest as HttpServletRequest
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header")

        if (!authHeader.startsWith("Bearer "))
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header")

        val token = authHeader.removePrefix("Bearer ").trim()

        if (!jwtTokenProvider.validateToken(token))
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token")

        val claims = jwtTokenProvider.parseToken(token)
        val memberId = claims["memberId"]?.toString()?.toLongOrNull()
            ?: throw IllegalArgumentException("JWT에서 memberId를 찾을 수 없습니다")

        return AuthMember(
            memberId = memberId,
            email = claims["email"].toString(),
            nickname = claims["nickname"].toString(),
            adult = claims["adult"].toString().toBoolean(),
            role = Role.valueOf(claims["role"].toString()),
        )
    }

}