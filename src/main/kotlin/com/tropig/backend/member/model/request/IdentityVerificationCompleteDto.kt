package com.tropig.backend.member.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "본인인증 완료 요청 - PortOne SDK 인증 후 identityVerificationId만 전송")
data class IdentityVerificationCompleteDto(
    @field:NotBlank(message = "identityVerificationId는 필수입니다.")
    @Schema(description = "PortOne SDK에서 발급된 본인인증 ID", example = "id-12345678-abcd-efgh")
    val identityVerificationId: String,
)
