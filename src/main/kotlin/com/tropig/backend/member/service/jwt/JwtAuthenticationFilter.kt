package com.tropig.backend.member.service.jwt

import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.repository.MemberRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtTokenProvider,
    private val memberRepository: MemberRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }

        if (token != null && jwtProvider.validateToken(token)) {
            val userId = jwtProvider.getUserIdFromToken(token)
            val user = memberRepository.findById(userId)
                .orElseThrow { RuntimeException("User not found") }

            val authUser = AuthMember(
                memberId = user.id,
                email = user.email,
                nickname = user.nickname,
                isAdult = user.isAdult,
                role = user.role
            )

            val auth = UsernamePasswordAuthenticationToken(authUser, null, listOf())
            SecurityContextHolder.getContext().authentication = auth  // 이렇게 써도 OK
        }

        filterChain.doFilter(request, response)
    }
}