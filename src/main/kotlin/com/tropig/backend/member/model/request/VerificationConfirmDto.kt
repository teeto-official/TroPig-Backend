package com.tropig.backend.member.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * 본인인증 확인 DTO (OTP 제출)
 */
data class VerificationConfirmDto(
    @field:NotBlank(message = "인증 ID는 필수입니다.")
    val verificationId: String,

    @field:NotBlank(message = "인증번호는 필수입니다.")
    @field:Pattern(
        regexp = "^[0-9]{6}$",
        message = "6자리 인증번호를 입력하세요."
    )
    val otp: String
)
