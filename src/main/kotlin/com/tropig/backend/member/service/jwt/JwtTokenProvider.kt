package com.tropig.backend.member.service.jwt

import com.tropig.backend.member.entity.Member
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
            .claim("email", member.email)
            .claim("snsProvider", member.snsProvider)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getClaims(token: String): Map<String, Any> {
        return Jwts.parserBuilder().setSigningKey(key).build()
            .parseClaimsJws(token).body
    }
}