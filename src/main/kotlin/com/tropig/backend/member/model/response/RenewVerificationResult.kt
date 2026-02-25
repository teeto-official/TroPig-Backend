package com.tropig.backend.member.model.response

import java.time.LocalDateTime

/**
 * 창작자 인증 갱신 결과 DTO
 */
data class RenewVerificationResult(val renewed: Boolean, val expiresAt: LocalDateTime, val message: String)
