package com.tropig.backend.member.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.entity.MemberAuthInfo
import com.tropig.backend.member.model.response.VerificationResult
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDateTime
import java.util.UUID

/**
 * 로컬 개발 전용 본인인증 Mock 컨트롤러
 * KCP 테스트 모드에서 PASS 앱 알림 없이 본인인증을 테스트하기 위한 엔드포인트입니다.
 * local, development 프로파일에서만 활성화됩니다.
 */
// @Profile("local", "development")
@ApiController
@RequestMapping("/api/member/identity-verification")
@Tag(name = "Identity Verification (Mock)", description = "로컬 개발 전용 본인인증 Mock API")
class MockIdentityVerificationController(
    private val memberRepository: MemberRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
) {

    @RequireAuth
    @PostMapping("/mock-complete")
    @Operation(
        summary = "[로컬 전용] 본인인증 Mock 완료",
        description = "KCP 테스트 환경에서 PASS 앱 없이 본인인증을 시뮬레이션합니다. local 프로파일에서만 동작합니다.",
    )
    fun mockComplete(@LoginMember authMember: AuthMember): ResponseEntity<VerificationResult> {
        val memberId = authMember.memberId

        val lastVerified = memberAuthInfoRepository.findByMemberId(memberId)
        if (lastVerified?.verifiedAt != null &&
            lastVerified.verifiedAt.plusYears(1).minusDays(7) >= LocalDateTime.now()
        ) {
            throw MemberException("이미 본인인증이 완료되었습니다.", MessageCode.ALREADY_VERIFIED)
        }

        val testBirthDate = "1995-01-01"
        val isAdult = MemberAuthInfo.isAdult(testBirthDate)

        val mockCiHash = MemberAuthInfo.sha256("MOCK_CI_${UUID.randomUUID()}")
        val mockDiHash = MemberAuthInfo.sha256("MOCK_DI_${UUID.randomUUID()}")

        val authInfo = lastVerified?.let {
            it.phoneNumber = "01012345678"
            it.ci = mockCiHash
            it.di = mockDiHash
            it.verifiedAt = LocalDateTime.now()
            it
        } ?: MemberAuthInfo(
            memberId = memberId,
            name = "테스트유저",
            birthDate = testBirthDate,
            phoneNumber = "01012345678",
            ci = mockCiHash,
            di = mockDiHash,
            verifiedAt = LocalDateTime.now(),
        )
        memberAuthInfoRepository.save(authInfo)

        val member = memberRepository.findById(memberId)
            .orElseThrow { MemberException("회원을 찾을 수 없습니다.", MessageCode.NOT_FOUND_MEMBER) }
        member.adult = isAdult
        memberRepository.save(member)

        return ResponseEntity.ok(
            VerificationResult(
                verified = true,
                adult = isAdult,
                name = authInfo.name,
                birthDate = authInfo.birthDate,
                phoneNumber = MemberAuthInfo.maskPhoneNumber(authInfo.phoneNumber),
                verifiedAt = authInfo.verifiedAt,
                message = "[테스트] 본인인증이 완료되었습니다.",
            ),
        )
    }
}
