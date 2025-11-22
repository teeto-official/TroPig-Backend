package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository: JpaRepository<RefreshToken, Long> {
    fun findByMemberId(memberId: Long): RefreshToken?
}