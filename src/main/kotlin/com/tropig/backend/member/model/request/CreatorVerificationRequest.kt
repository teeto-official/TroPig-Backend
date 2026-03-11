package com.tropig.backend.member.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 창작자 인증 요청 DTO
 */
data class CreatorVerificationRequest(
    @field:NotBlank(message = "은행명은 필수입니다")
    @field:Size(max = 50, message = "은행명은 50자 이하여야 합니다")
    val bankName: String,

    @field:NotBlank(message = "계좌번호는 필수입니다")
    @field:Pattern(
        regexp = "^[0-9]{10,14}$",
        message = "계좌번호는 10-14자리 숫자여야 합니다",
    )
    val accountNumber: String,

    val accountHolder: String = "",
)
