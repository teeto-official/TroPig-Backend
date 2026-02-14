package com.tropig.backend.member.model.response

import com.tropig.backend.member.enums.Role
import java.time.LocalDateTime

/**
 * 창작자 인증 결과 DTO
 */
data class CreatorVerificationResult(
    val verified: Boolean,
    val role: Role,
    val expiresAt: LocalDateTime,
    val message: String,
    val partnerId: String?
)
