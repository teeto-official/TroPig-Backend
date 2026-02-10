package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.member.entity.AuthMember
import com.tropig.backend.member.model.request.ChangeAccountRequest
import com.tropig.backend.member.model.request.CreatorVerificationRequest
import com.tropig.backend.member.model.response.ChangeAccountResult
import com.tropig.backend.member.model.response.CreatorVerificationResult
import com.tropig.backend.member.model.response.CreatorVerificationStatusResponse
import com.tropig.backend.member.model.response.RenewVerificationResult
import com.tropig.backend.member.service.CreatorVerificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 창작자 인증 API
 * 본인인증 후 은행 계좌 등록 및 PortOne 파트너 등록
 */
@ApiController
@RequestMapping("/api/member/creator-verification")
@Tag(name = "Creator Verification", description = "창작자 인증 API")
class CreatorVerificationController(
    private val creatorVerificationService: CreatorVerificationService
) {

    /**
     * 창작자 인증 요청
     * POST /api/member/creator-verification
     */
    @RequireAuth
    @PostMapping
    @Operation(summary = "창작자 인증", description = "은행 계좌 등록 및 파트너 등록으로 창작자 인증")
    fun verifyCreator(
        @AuthenticationPrincipal @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: CreatorVerificationRequest
    ): CreatorVerificationResult {
        return creatorVerificationService.verifyCreator(authMember.memberId, request)
    }

    /**
     * 창작자 인증 상태 조회
     * GET /api/member/creator-verification
     */
    @RequireAuth
    @GetMapping
    @Operation(summary = "창작자 인증 상태 조회", description = "현재 창작자 인증 상태 및 계좌 정보 조회")
    fun getVerificationStatus(
        @AuthenticationPrincipal @LoginMember authMember: AuthMember
    ): CreatorVerificationStatusResponse {
        return creatorVerificationService.getVerificationStatus(authMember.memberId)
    }

    /**
     * 창작자 인증 갱신
     * POST /api/member/creator-verification/renew
     */
    @RequireAuth
    @PostMapping("/renew")
    @Operation(summary = "창작자 인증 갱신", description = "만료 전 인증 갱신 (기존 계좌 정보로 1년 연장)")
    fun renewVerification(
        @AuthenticationPrincipal @LoginMember authMember: AuthMember
    ): RenewVerificationResult {
        return creatorVerificationService.renewVerification(authMember.memberId)
    }

    /**
     * 계좌 정보 변경
     * PUT /api/member/creator-verification/account
     */
    @RequireAuth
    @PutMapping("/account")
    @Operation(summary = "계좌 정보 변경", description = "등록된 계좌 정보 변경 (30일 lockout 적용)")
    fun changeAccount(
        @AuthenticationPrincipal @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: ChangeAccountRequest
    ): ChangeAccountResult {
        return creatorVerificationService.changeAccount(authMember.memberId, request)
    }
}
