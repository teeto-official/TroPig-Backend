package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.model.request.CreatorEmailTotpConfirmRequest
import com.tropig.backend.member.model.response.CreatorEmailTotpConfirmResult
import com.tropig.backend.member.model.response.CreatorEmailTotpRequestResult
import com.tropig.backend.member.service.CreatorEmailTotpService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/api/member/creator/totp")
@Tag(name = "Creator Email TOTP", description = "창작자 이메일 TOTP 인증 API")
class CreatorEmailTotpController(private val creatorEmailTotpService: CreatorEmailTotpService) {
    @RequireAuth
    @PostMapping("/send")
    @Operation(
        summary = "창작자 이메일 TOTP 발송",
        description = "회원 이메일로 6자리 인증번호를 발송합니다. 1분간 재발송할 수 없습니다.",
    )
    fun sendTotp(@LoginMember authMember: AuthMember): CreatorEmailTotpRequestResult =
        creatorEmailTotpService.sendTotp(authMember.memberId)

    @RequireAuth
    @PostMapping("/confirm")
    @Operation(
        summary = "창작자 이메일 TOTP 인증",
        description = "이메일 인증번호를 확인하고 창작자 권한을 부여합니다.",
    )
    fun confirmTotp(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: CreatorEmailTotpConfirmRequest,
    ): CreatorEmailTotpConfirmResult = creatorEmailTotpService.confirmTotp(authMember.memberId, request)
}
