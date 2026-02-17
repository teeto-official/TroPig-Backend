package com.tropig.backend.member.model.response

import java.time.LocalDateTime

/**
 * OTP 재전송 결과
 */
data class VerificationResendResult(
    /** 재전송 성공 여부 */
    val sent: Boolean,

    /** 새로운 만료 시간 */
    val expiresAt: LocalDateTime,

    /** 사용자 메시지 */
    val message: String,

    /** 남은 재전송 횟수 */
    val remainingResends: Int? = null
)
