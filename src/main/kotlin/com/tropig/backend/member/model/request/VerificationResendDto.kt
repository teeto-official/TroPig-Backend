package com.tropig.backend.member.model.request

import jakarta.validation.constraints.NotBlank

/**
 * 본인인증 OTP 재전송 요청 DTO
 */
data class VerificationResendDto(
    @field:NotBlank(message = "인증 ID는 필수입니다.")
    val verificationId: String
)
