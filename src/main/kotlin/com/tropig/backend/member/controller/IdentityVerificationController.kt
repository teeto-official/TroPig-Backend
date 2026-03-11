package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.model.request.IdentityVerificationCompleteDto
import com.tropig.backend.member.model.request.VerificationConfirmDto
import com.tropig.backend.member.model.request.VerificationRequestDto
import com.tropig.backend.member.model.request.VerificationResendDto
import com.tropig.backend.member.model.response.*
import com.tropig.backend.member.service.IdentityVerificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 본인인증 컨트롤러
 * 사용자 신원 확인 및 성인 인증 처리
 */
@ApiController
@RequestMapping("/api/member/identity-verification")
@Tag(name = "Identity Verification", description = "본인인증 API")
class IdentityVerificationController(private val identityVerificationService: IdentityVerificationService) {

    @RequireAuth
    @PostMapping("/request")
    @Operation(
        summary = "본인인증 요청",
        description = "휴대폰 본인인증을 시작합니다. OTP가 SMS로 전송됩니다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "인증 요청 성공 (OTP 전송됨)",
                content = [Content(schema = Schema(implementation = VerificationRequestResult::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (유효하지 않은 전화번호, 주민등록번호 등)",
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 본인인증이 완료된 사용자",
            ),
            ApiResponse(
                responseCode = "503",
                description = "외부 서비스 오류 (PortOne API)",
            ),
        ],
    )
    fun requestVerification(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: VerificationRequestDto,
    ): ResponseEntity<VerificationRequestResult> {
        val result = identityVerificationService.requestVerification(authMember.memberId, request)
        return ResponseEntity.ok(result)
    }

    @RequireAuth
    @PostMapping("/confirm")
    @Operation(
        summary = "본인인증 확인",
        description = "SMS로 받은 OTP 코드를 제출하여 본인인증을 완료합니다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "본인인증 완료",
                content = [Content(schema = Schema(implementation = VerificationResult::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 OTP 또는 만료된 세션",
            ),
            ApiResponse(
                responseCode = "404",
                description = "인증 세션을 찾을 수 없음",
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 CI/DI (이미 다른 계정에서 인증됨)",
            ),
            ApiResponse(
                responseCode = "429",
                description = "OTP 시도 횟수 초과",
            ),
        ],
    )
    fun confirmVerification(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: VerificationConfirmDto,
    ): ResponseEntity<VerificationResult> {
        val result = identityVerificationService.confirmVerification(authMember.memberId, request)
        return ResponseEntity.ok(result)
    }

    @RequireAuth
    @PostMapping("/complete")
    @Operation(
        summary = "본인인증 완료 (SDK 방식)",
        description = "프론트엔드에서 PortOne SDK로 본인인증 완료 후 identityVerificationId를 전달하여 인증 결과를 저장합니다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "본인인증 완료",
                content = [Content(schema = Schema(implementation = VerificationResult::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (인증이 완료되지 않은 identityVerificationId 등)",
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 본인인증이 완료되었거나 중복된 CI/DI",
            ),
            ApiResponse(
                responseCode = "503",
                description = "외부 서비스 오류 (PortOne API)",
            ),
        ],
    )
    fun completeVerification(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: IdentityVerificationCompleteDto,
    ): ResponseEntity<VerificationResult> {
        val result = identityVerificationService.completeVerification(authMember.memberId, request)
        return ResponseEntity.ok(result)
    }

    @RequireAuth
    @PostMapping("/resend")
    @Operation(
        summary = "OTP 재전송",
        description = "본인인증 OTP를 재전송합니다. (최대 3회)",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OTP 재전송 성공",
                content = [Content(schema = Schema(implementation = VerificationResendResult::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
            ),
            ApiResponse(
                responseCode = "404",
                description = "인증 세션을 찾을 수 없음",
            ),
            ApiResponse(
                responseCode = "429",
                description = "재전송 횟수 초과 (최대 3회)",
            ),
        ],
    )
    fun resendOtp(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: VerificationResendDto,
    ): ResponseEntity<VerificationResendResult> {
        val result = identityVerificationService.resendOtp(authMember.memberId, request)
        return ResponseEntity.ok(result)
    }

    @RequireAuth
    @GetMapping
    @Operation(
        summary = "본인인증 상태 조회",
        description = "현재 사용자의 본인인증 상태를 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = VerificationStatusResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "회원을 찾을 수 없음",
            ),
        ],
    )
    fun getVerificationStatus(@LoginMember authMember: AuthMember): ResponseEntity<VerificationStatusResponse> {
        val result = identityVerificationService.getVerificationStatus(authMember.memberId)
        return ResponseEntity.ok(result)
    }
}
