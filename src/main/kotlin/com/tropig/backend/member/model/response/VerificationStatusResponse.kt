package com.tropig.backend.member.model.response

import java.time.LocalDateTime

/**
 * 본인인증 상태 조회 결과
 */
data class VerificationStatusResponse(
    /** 인증 완료 여부 */
    val verified: Boolean,

    /** 성인 여부 */
    val adult: Boolean,

    /** 인증 완료 시간 */
    val verifiedAt: LocalDateTime?,

    /** 실명 */
    val name: String?,

    /** 휴대폰 번호 (마스킹 처리) */
    val phoneNumber: String?,

    /** 생년월일 */
    val birthDate: String?,

    /** 나이 */
    val age: Int?
)
