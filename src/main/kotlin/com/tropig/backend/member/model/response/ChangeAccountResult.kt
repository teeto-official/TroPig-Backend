package com.tropig.backend.member.model.response

import java.time.LocalDateTime

/**
 * 계좌 변경 결과 DTO
 */
data class ChangeAccountResult(
    val updated: Boolean,
    val message: String,
    val lockedUntil: LocalDateTime,
    val newAccountInfo: MaskedAccountInfo
)
