package com.tropig.backend.member.model.response

import com.tropig.backend.member.enums.VerificationMethod
import java.time.LocalDateTime

/**
 * 본인인증 요청 결과
 */
data class VerificationRequestResult(
    /** 인증 세션 ID (확인 시 필요) */
    val verificationId: String,

    /** OTP 만료 시간 */
    val expiresAt: LocalDateTime,

    /** 인증 방법 */
    val method: VerificationMethod,

    /** 사용자 메시지 */
    val message: String,
)
