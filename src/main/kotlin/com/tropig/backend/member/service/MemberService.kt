package com.tropig.backend.member.service

import com.tropig.backend.common.enums.BankCode
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.exception.AuthenticatedException
import com.tropig.backend.common.util.DateUtil
import com.tropig.backend.common.util.SecurityUtil
import com.tropig.backend.common.util.StringUtil
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.entity.MemberAuthInfo
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.AccountAuthResponse
import com.tropig.backend.member.model.response.IdentityVerificationResponse
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.member.service.jwt.JwtTokenProvider
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val stringUtil: StringUtil,
) {
    fun getUserById(id: Long): Member {
        return memberRepository.findById(id).orElse(null)?.apply {
            if (this.deletedAt != null) {
                throw NotFoundException(
                    message = "${MessageCode.WITHDRAWAL_MEMBER.name}: memberId = $id",
                    code = MessageCode.WITHDRAWAL_MEMBER
                )
            }
        } ?: throw NotFoundException(
            message = "${MessageCode.NOT_FOUND_MEMBER.name}: memberId = $id",
            code = MessageCode.NOT_FOUND_MEMBER
        )
    }
    
    fun getUserByEmail(email: String): Member? {
        return memberRepository.findByEmail(email)
    }

    @Transactional
    fun createMember(request: SignUpRequest): Member {
        if (memberRepository.existsByEmail(request.email)) {
            // TODO: 재가입 여부 체크 로직 추가 예정
            throw IllegalArgumentException("User with email ${request.email} already exists")
        }
        val nickname = request.nickname ?: makeNickname()

        return memberRepository.save(request.toEntity(nickname))
    }

    fun signUp(request: SignUpRequest): TokenResponse {
        val member = createMember(request)
        val now = Date()
        val token = jwtTokenProvider.getToken(member, now)

        return TokenResponse(
            token.first,
            token.second,
            Instant.ofEpochMilli(token.third)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime()
        )
    }

    fun signIn(request: SignInRequest): TokenResponse {
        val member = loginMember(request)

        val token = jwtTokenProvider.getToken(member, Date())

        return TokenResponse(
            token.first,
            token.second,
            Instant.ofEpochMilli(token.third)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime()
        )
    }

    private fun loginMember(request: SignInRequest): Member {
        val member = memberRepository.findBySnsIdAndSnsProviderAndEmail(
            request.snsId,
            request.snsProvider,
            request.email
        ) ?: throw NotFoundException(
            message = "가입된 정보가 없는 유저입니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )

        member.deletedAt?.let {
            if (it.plusDays(7) > LocalDateTime.now()) {
                throw IllegalArgumentException("탈퇴한 회원입니다.")
            }
        }

        return member
    }

    /**
     * 🔒 계좌 인증 결과 저장
     */
    @Transactional
    fun authenticateAccount(
        authInfo: MemberAuthInfo,
        account: AccountAuthResponse,
    ) {
        // 중복 계좌 인증 방지
        if (authInfo.authCreatorAt != null) {
            throw AuthenticatedException(
                "이미 계좌 인증이 완료된 사용자입니다. memberId=${authInfo.memberId}",
                MessageCode.ALREADY_AUTHENTICATED_BANK_ACCOUNT
            )
        }

        val aad = getAad(authInfo.memberId)
        val encryptedHolderName = SecurityUtil.encrypt(account.holderName, aad)
        if (authInfo.name != null && authInfo.name == encryptedHolderName) {
            authInfo.bankCode = BankCode.valueOf(account.bankName)
            authInfo.bankAccount = SecurityUtil.encrypt(account.bankAccount, aad)
            authInfo.authCreatorAt = now()
        } else if (authInfo.name != null) {
            throw AuthenticatedException(
                "계좌주의 이름이 본인인증 이름과 다릅니다. memberId=${authInfo.memberId}",
                MessageCode.UNEQUAL_MEMBER_NAME
            )
        } else {
            throw AuthenticatedException(
                "본인인증이 먼저 필요합니다. memberId=${authInfo.memberId}",
                MessageCode.UNAUTHENTICATED_ADULT
            )
        }
    }

    /**
     * 🔒 본인 인증 결과 저장
     */
    @Transactional
    fun authenticateUser(
        authInfo: MemberAuthInfo,
        userId: Long,
        response: IdentityVerificationResponse
    ) {
        // 중복 본인 인증 방지
        if (authInfo.authUserAt != null && authInfo.authUserAt!!.plusYears(1) >= now()) {
            throw AuthenticatedException(
                "이미 본인 인증이 완료된 사용자입니다. userId=$userId",
                MessageCode.ALREADY_AUTHENTICATED_ADULT
            )
        }

        val birthedAt = LocalDate.parse(response.verifiedCustomer.birthDate!!).atStartOfDay()
        if (birthedAt.plusYears(19).year >= LocalDate.now().year) {
            val aad = getAad(userId)
            authInfo.di = response.verifiedCustomer.di!!
            authInfo.name = SecurityUtil.encrypt(response.verifiedCustomer.name!!, aad)
            authInfo.birthedAt = birthedAt
            authInfo.authUserAt = now()
        } else {
            throw AuthenticatedException(
                "미성년자 회원입니다. memberId=$userId",
                MessageCode.NOT_ADULT_USER
            )
        }
    }

    /**
     * 🔑 내부 공통 메서드
     * - row 있으면 FOR UPDATE
     * - 없으면 생성 후 다시 FOR UPDATE
     */
    fun findOrCreateForUpdate(memberId: Long): MemberAuthInfo {
        memberAuthInfoRepository.findByMemberIdForUpdate(memberId)?.let {
            return it
        }

        // 최초 생성 (락 없음)
        memberAuthInfoRepository.save(MemberAuthInfo(memberId = memberId))

        // 생성 직후 다시 락 조회
        return memberAuthInfoRepository.findByMemberIdForUpdate(memberId)
            ?: throw IllegalStateException("MemberAuthInfo 생성 실패. userId=$memberId")
    }

    private fun now(): LocalDateTime =
        LocalDateTime.now(ZoneId.of("Asia/Seoul"))


    @Transactional
    fun updateUser(member: Member, request: UpdateMemberRequest): Member {
        val updatedUser = member.copy(
            nickname = request.nickname ?: member.nickname,
        ).apply {
            request.bio?.let { this.bio = it }

            if (request.favoriteRules.isNotEmpty()) {
                this.favoriteRules = request.favoriteRules.joinToString()
            }

            if (request.favoriteGenres.isNotEmpty()) {
                this.favoriteGenres = request.favoriteGenres.joinToString()
            }

            if (request.isMarketing && (this.marketingAt == null)) {
                this.marketingAt = LocalDateTime.now()
            } else if (!request.isMarketing && (this.marketingAt != null)) {
                        this.marketingAt = null
            }
        }
        
        return memberRepository.save(updatedUser)
    }

    fun getWritersName(writerIds: List<Long>): Map<Long, String> {
        return memberRepository.findByIdInAndRoleAndDeletedAtIsNull(writerIds, Role.CREATOR)
            .associate { it.id to it.nickname }
    }
    
    fun deleteUser(id: Long): Boolean {
        return if (memberRepository.existsById(id)) {
            memberRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    private fun makeNickname(): String {
        val (adjustList, nounList) = stringUtil.getWordLists()
        val adjust = adjustList.random()
        val noun = nounList.random()
        val num = (1..9999).random()
        return adjust + noun + num.toString().padStart(4, '0')
    }

    private fun getAad(memberId: Long): ByteArray =
        "user:$memberId".toByteArray()
}
