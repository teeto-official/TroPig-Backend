package com.tropig.backend.member.model.response

import com.tropig.backend.member.enums.Role
import java.time.LocalDateTime

/**
 * 창작자 인증 상태 조회 응답 DTO
 */
data class CreatorVerificationStatusResponse(
    val verified: Boolean,
    val role: Role,
    val expiresAt: LocalDateTime?,
    val daysUntilExpiry: Int?,
    val expired: Boolean = false,
    val canRenew: Boolean,
    val accountInfo: MaskedAccountInfo?,
    val lastChangedAt: LocalDateTime?,
    val canChangeAccount: Boolean,
    val nextChangeAvailableAt: LocalDateTime?,
)

/**
 * 마스킹된 계좌 정보 DTO
 */
data class MaskedAccountInfo(
    val bankName: String,
    val accountNumber: String, // Masked: "110-****-*000"
    val accountHolder: String, // Masked: "홍*동"
)
