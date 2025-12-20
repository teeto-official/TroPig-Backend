package com.tropig.backend.member.service.jwt

import com.tropig.backend.member.entity.Member
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
) {

    private val keyBytes = Decoders.BASE64.decode(secretKey)
    private val key: Key = Keys.hmacShaKeyFor(keyBytes)

    fun getToken(member: Member, now: Date): Triple<String, String, Long> {
        val access = generateToken(accessExpiration, now, member)
        val refresh = generateToken(refreshExpiration, now, member)
        val expireAt = accessExpiration + now.time
        return Triple(access, refresh, expireAt)
    }

    private fun generateToken(expiration: Long, now: Date, member: Member): String {
        return Jwts.builder()
            .setSubject(member.id.toString())
            .claim("memberId", member.id)
            .claim("email", member.email)
            .claim("snsProvider", member.snsProvider)
            .claim("role", member.role)
            .claim("adult", member.adult)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }


    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        return claims.subject.toLong()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun parseToken(token: String): Map<String, Any> {
        try {
            val claimsJws = Jwts.parserBuilder()
                .setSigningKey(secretKey.toByteArray())
                .build()
                .parseClaimsJws(token)

            return claimsJws.body.entries.associate { it.key to it.value }
        } catch (e: ExpiredJwtException) {
            throw IllegalArgumentException("JWT가 만료되었습니다")
        } catch (e: JwtException) {
            throw IllegalArgumentException("JWT가 유효하지 않습니다")
        }
    }
}