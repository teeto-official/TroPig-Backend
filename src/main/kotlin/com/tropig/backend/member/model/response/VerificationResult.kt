package com.tropig.backend.member.model.response

import java.time.LocalDateTime

/**
 * 본인인증 확인 결과
 */
data class VerificationResult(
    /** 인증 성공 여부 */
    val verified: Boolean,

    /** 성인 여부 (만 19세 이상) */
    val adult: Boolean,

    /** 실명 */
    val name: String,

    /** 생년월일 (YYYY-MM-DD) */
    val birthDate: String,

    /** 휴대폰 번호 (마스킹 처리) */
    val phoneNumber: String,

    /** 인증 완료 시간 */
    val verifiedAt: LocalDateTime,

    /** 사용자 메시지 */
    val message: String
)
