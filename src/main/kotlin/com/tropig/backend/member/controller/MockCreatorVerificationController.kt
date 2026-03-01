package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.util.EncryptionService
import com.tropig.backend.member.entity.MemberAccount
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.ChangeAccountRequest
import com.tropig.backend.member.model.request.CreatorVerificationRequest
import com.tropig.backend.member.model.response.ChangeAccountResult
import com.tropig.backend.member.model.response.CreatorVerificationResult
import com.tropig.backend.member.model.response.CreatorVerificationStatusResponse
import com.tropig.backend.member.model.response.MaskedAccountInfo
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * 로컬 개발 전용 창작자 인증 Mock 컨트롤러
 * PortOne 은행 계좌 실명 인증 및 파트너 등록 없이 창작자 인증을 테스트하기 위한 엔드포인트입니다.
 * 계좌번호는 AES-256-GCM으로 암호화하여 member_auth_info.bank_account에 저장됩니다.
 * 계좌 변경 잠금은 auth_creator_at 기준 30일입니다.
 * local, development 프로파일에서만 활성화됩니다.
 */
@Profile("local", "development")
@ApiController
@RequestMapping("/api/member/creator-verification/mock")
@Tag(name = "Creator Verification (Mock)", description = "로컬 개발 전용 창작자 인증 Mock API")
class MockCreatorVerificationController(
    private val memberRepository: MemberRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val encryptionService: EncryptionService,
) {
    /**
     * bank_account 필드 저장 포맷: "{bankName}:{encryptedAccountNumber}"
     * 은행명은 평문, 계좌번호는 AES-256-GCM 암호화
     */
    private fun encodeBankAccount(bankName: String, encryptedAccountNumber: String) =
        "$bankName:$encryptedAccountNumber"

    private fun decodeBankAccount(raw: String): Pair<String, String> {
        val idx = raw.indexOf(':')
        require(idx > 0) { "Invalid bankAccount format" }
        return raw.substring(0, idx) to raw.substring(idx + 1)
    }

    /**
     * [로컬 전용] 창작자 인증 / 갱신 Mock
     * - 본인인증(member_auth_info 존재)이 선행되어야 합니다.
     * - 창작자 미인증 상태: 계좌 정보로 최초 창작자 인증을 수행합니다.
     * - 창작자 인증 완료 상태 + 갱신 가능 기간(만료 30일 이내): 기존 계좌 정보로 인증을 갱신합니다.
     * - 창작자 인증 완료 상태 + 갱신 불가 기간: CREATOR_ALREADY_VERIFIED 에러를 반환합니다.
     */
    @RequireAuth
    @PostMapping
    @Operation(
        summary = "[로컬 전용] 창작자 인증 / 갱신 Mock",
        description = "창작자 미인증 시 최초 인증, 갱신 가능 기간(만료 30일 이내)이면 갱신을 수행합니다. local 프로파일에서만 동작합니다.",
    )
    fun mockVerifyOrRenewCreator(
        @LoginMember authMember: AuthMember,
        @RequestBody request: CreatorVerificationRequest,
    ): CreatorVerificationResult {
        val memberId = authMember.memberId

        val member = memberRepository.findById(memberId).orElseThrow {
            MemberException("회원을 찾을 수 없습니다.", MessageCode.MEMBER_NOT_FOUND)
        }

        // 본인인증 미완료 차단
        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)
            ?: throw MemberException("본인인증이 필요합니다.", MessageCode.IDENTITY_VERIFICATION_REQUIRED)

        val now = LocalDateTime.now()

        if (authInfo.creator && (authInfo.authCreatorAt?.plusDays(30) ?: LocalDateTime.now()) > LocalDateTime.now()) {
            throw MemberException("이미 창작자 인증이 완료되었습니다.", MessageCode.CREATOR_ALREADY_VERIFIED)
        }

        val message = if (authInfo.authCreatorAt != null) {
            "[테스트] 창작자 인증이 갱신되었습니다."
        } else {
            "[테스트] 창작자 인증이 완료되었습니다."
        }
        val encryptedAccountNumber = encryptionService.encrypt(request.accountNumber)
        authInfo.authCreatorAt = now
        authInfo.creator = true
        authInfo.bankAccount = encodeBankAccount(request.bankName, encryptedAccountNumber)

        memberAuthInfoRepository.save(authInfo)

        member.role = Role.CREATOR
        memberRepository.save(member)

        val newExpiresAt = now.plusDays(30)
        return CreatorVerificationResult(
            verified = true,
            role = member.role,
            expiresAt = newExpiresAt,
            message = message,
            partnerId = "mock_partner_${UUID.randomUUID()}",
        )
    }

    /**
     * [로컬 전용] 창작자 인증 상태 조회 Mock
     */
    @RequireAuth
    @GetMapping
    @Operation(
        summary = "[로컬 전용] 창작자 인증 상태 조회 Mock",
        description = "창작자 인증 상태를 조회합니다. local 프로파일에서만 동작합니다.",
    )
    fun mockGetVerificationStatus(@LoginMember authMember: AuthMember): CreatorVerificationStatusResponse {
        val memberId = authMember.memberId

        val member = memberRepository.findById(memberId).orElseThrow {
            MemberException("회원을 찾을 수 없습니다.", MessageCode.MEMBER_NOT_FOUND)
        }

        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)

        if (authInfo == null || !authInfo.creator || authInfo.authCreatorAt == null) {
            return CreatorVerificationStatusResponse(
                verified = false,
                role = member.role,
                expiresAt = null,
                daysUntilExpiry = null,
                expired = false,
                canRenew = false,
                accountInfo = null,
                partnerId = null,
                lastChangedAt = null,
                canChangeAccount = false,
                nextChangeAvailableAt = null,
            )
        }

        val expiresAt = authInfo.authCreatorAt!!.plusYears(MemberAccount.VERIFICATION_VALIDITY_YEARS)
        val now = LocalDateTime.now()
        val daysUntilExpiry = ChronoUnit.DAYS.between(now, expiresAt).toInt()
        val expired = now.isAfter(expiresAt)
        val canRenew = daysUntilExpiry <= MemberAccount.RENEWAL_WINDOW_DAYS

        // 계좌 변경 잠금: auth_creator_at 기준 30일
        val daysSinceCreatorAt = ChronoUnit.DAYS.between(authInfo.authCreatorAt, now)
        val canChangeAccount = daysSinceCreatorAt >= MemberAccount.ACCOUNT_CHANGE_LOCKOUT_DAYS
        val nextChangeAvailableAt = if (canChangeAccount) {
            null
        } else {
            authInfo.authCreatorAt!!.plusDays(MemberAccount.ACCOUNT_CHANGE_LOCKOUT_DAYS)
        }

        val accountInfo = authInfo.bankAccount?.let { raw ->
            val (bankName, encryptedAccountNumber) = decodeBankAccount(raw)
            MaskedAccountInfo(
                bankName = bankName,
                accountNumber = maskAccountNumber(encryptionService.decrypt(encryptedAccountNumber)),
                accountHolder = maskAccountHolder(authInfo.name),
            )
        }

        return CreatorVerificationStatusResponse(
            verified = true,
            role = member.role,
            expiresAt = expiresAt,
            daysUntilExpiry = daysUntilExpiry,
            expired = expired,
            canRenew = canRenew,
            accountInfo = accountInfo,
            partnerId = null,
            lastChangedAt = authInfo.authCreatorAt,
            canChangeAccount = canChangeAccount,
            nextChangeAvailableAt = nextChangeAvailableAt,
        )
    }

    /**
     * [로컬 전용] 계좌 정보 변경 Mock
     * - 계좌 변경 잠금: auth_creator_at 기준 30일
     * - 계좌번호는 AES-256-GCM으로 암호화하여 member_auth_info.bank_account에 저장됩니다.
     */
    @RequireAuth
    @PutMapping("/account")
    @Operation(
        summary = "[로컬 전용] 계좌 정보 변경 Mock",
        description = "PortOne 연동 없이 계좌 정보 변경을 시뮬레이션합니다. 계좌 변경 잠금은 auth_creator_at 기준 30일입니다. local 프로파일에서만 동작합니다.",
    )
    fun mockChangeAccount(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: ChangeAccountRequest,
    ): ChangeAccountResult {
        val memberId = authMember.memberId

        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)
            ?: throw MemberException("본인인증이 필요합니다.", MessageCode.IDENTITY_VERIFICATION_REQUIRED)

        if (!authInfo.creator || authInfo.authCreatorAt == null) {
            throw MemberException("창작자 인증 정보가 없습니다.", MessageCode.CREATOR_VERIFICATION_NOT_FOUND)
        }

        // 계좌 변경 잠금 체크: auth_creator_at 기준 30일
        val now = LocalDateTime.now()
        val daysSinceCreatorAt = ChronoUnit.DAYS.between(authInfo.authCreatorAt, now)
        if (daysSinceCreatorAt < MemberAccount.ACCOUNT_CHANGE_LOCKOUT_DAYS) {
            throw MemberException("계좌 변경 후 30일간 재변경이 불가능합니다.", MessageCode.ACCOUNT_CHANGE_LOCKED)
        }

        if (request.accountHolder != authInfo.name) {
            throw MemberException(
                "예금주명이 본인인증 실명과 일치하지 않습니다.",
                MessageCode.ACCOUNT_HOLDER_MISMATCH,
            )
        }

        // 기존 계좌와 동일 여부 확인
        if (authInfo.bankAccount != null) {
            val (existingBankName, existingEncrypted) = decodeBankAccount(authInfo.bankAccount!!)
            val existingAccountNumber = encryptionService.decrypt(existingEncrypted)
            if (existingAccountNumber == request.accountNumber && existingBankName == request.bankName) {
                throw MemberException("기존 계좌와 동일합니다.", MessageCode.ACCOUNT_UNCHANGED)
            }
        }

        val encryptedAccountNumber = encryptionService.encrypt(request.accountNumber)

        // auth_creator_at을 현재 시각으로 갱신하여 잠금 기간 리셋
        memberAuthInfoRepository.save(
            authInfo.copy(
                authCreatorAt = now,
                bankAccount = encodeBankAccount(request.bankName, encryptedAccountNumber),
            ),
        )

        val lockedUntil = now.plusDays(MemberAccount.ACCOUNT_CHANGE_LOCKOUT_DAYS)

        return ChangeAccountResult(
            updated = true,
            message = "[테스트] 계좌 정보가 변경되었습니다. 30일간 재변경이 불가능합니다.",
            lockedUntil = lockedUntil,
            newAccountInfo = MaskedAccountInfo(
                bankName = request.bankName,
                accountNumber = maskAccountNumber(request.accountNumber),
                accountHolder = maskAccountHolder(request.accountHolder),
            ),
        )
    }

    private fun maskAccountNumber(accountNumber: String): String = if (accountNumber.length >= 6) {
        "${accountNumber.substring(0, 3)}-****-*${accountNumber.substring(accountNumber.length - 3)}"
    } else {
        "****"
    }

    private fun maskAccountHolder(name: String): String = if (name.length >= 3) {
        "${name.first()}${"*".repeat(name.length - 2)}${name.last()}"
    } else if (name.length == 2) {
        "${name.first()}*"
    } else {
        name
    }
}
