package com.tropig.backend.member.service.jwt

import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.member.service.MemberCacheService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtTokenProvider,
    private val memberRepository: MemberRepository,
    private val memberCacheService: MemberCacheService,
    private val tokenBlacklistService: TokenBlacklistService,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }

        if (token != null) {
            if (jwtProvider.validateToken(token) && !tokenBlacklistService.isBlacklisted(token)) {
                try {
                    val userId = jwtProvider.getUserIdFromToken(token)

                    val authUser = memberCacheService.getCachedMember(userId)
                        ?: memberRepository.findById(userId)
                            .orElseThrow { RuntimeException("User not found: $userId") }
                            .let { user ->
                                AuthMember(
                                    memberId = user.id,
                                    email = user.email,
                                    nickname = user.nickname,
                                    adult = user.adult,
                                    role = user.role,
                                ).also { memberCacheService.cacheMember(userId, it) }
                            }

                    val auth = UsernamePasswordAuthenticationToken(authUser, null, listOf())
                    SecurityContextHolder.getContext().authentication = auth
                } catch (e: Exception) {
                    log.warn("[JWT] 인증 처리 실패 - uri={} error={}", request.requestURI, e.message)
                }
            } else {
                log.debug("[JWT] 토큰 검증 실패 - uri={} (만료되었거나 유효하지 않은 토큰)", request.requestURI)
            }
        }

        filterChain.doFilter(request, response)
    }
}
