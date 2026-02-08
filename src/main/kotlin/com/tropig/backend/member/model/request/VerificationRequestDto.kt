package com.tropig.backend.member.model.request

import com.tropig.backend.member.enums.Carrier
import com.tropig.backend.member.enums.VerificationMethod
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 본인인증 요청 DTO
 */
data class VerificationRequestDto(
    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Size(min = 2, max = 20, message = "이름은 2-20자여야 합니다.")
    val name: String,

    @field:NotBlank(message = "휴대폰 번호는 필수입니다.")
    @field:Pattern(
        regexp = "^01[0-9]{8,9}$",
        message = "유효한 휴대폰 번호를 입력하세요. (예: 01012345678)"
    )
    val phoneNumber: String,

    @field:NotBlank(message = "주민등록번호 앞 7자리는 필수입니다.")
    @field:Pattern(
        regexp = "^[0-9]{7}$",
        message = "주민등록번호 앞 7자리를 입력하세요. (예: 9001011)"
    )
    val idNumber: String,

    @field:NotNull(message = "통신사는 필수입니다.")
    val carrier: Carrier,

    @field:NotNull(message = "인증 방법은 필수입니다.")
    val method: VerificationMethod
)
