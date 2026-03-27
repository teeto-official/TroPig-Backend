package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.IllegalArgumentException
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.util.StringUtil
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.entity.MemberAuthInfo
import com.tropig.backend.member.entity.WithdrawMember
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.member.repository.WithdrawMemberRepository
import com.tropig.backend.member.service.jwt.JwtTokenProvider
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val withdrawMemberRepository: WithdrawMemberRepository,
    private val contentRepository: ContentRepository,
    private val s3Service: S3Service,
    private val jwtTokenProvider: JwtTokenProvider,
    private val stringUtil: StringUtil,
) {

    companion object {
        private const val REJOIN_DAYS: Long = 7
    }

    fun getUserByEmail(email: String): Member? = memberRepository.findByEmail(email)

    fun getUserById(id: Long): Member? = memberRepository.findMemberByIdAndDeletedAtIsNull(id)

    fun findMemberAuthInfo(memberId: Long): MemberAuthInfo? = memberAuthInfoRepository.findByMemberId(memberId)

    @Transactional
    fun createMember(request: SignUpRequest): Member {
        memberRepository.findByEmail(request.email)?.let {
            it.deletedAt?.let { date ->
                if (date.plusDays(REJOIN_DAYS) >= LocalDateTime.now()) {
                    // 탈퇴일로부터 7일이 지나지 않음
                    throw MemberException(
                        message = "탈퇴 후, ${REJOIN_DAYS}일간 재가입이 불가합니다. 탈퇴일: $date",
                        code = MessageCode.CANNOT_REJOIN_MEMBER,
                    )
                }
            } ?: throw MemberException(
                "해당 이메일을 사용하는 유저가 있습니다. email: ${request.email}",
                MessageCode.ALREADY_EXISTS_MEMBER,
            )
        }

        val nickname = generateSequence { makeNickname() }
            .first { !memberRepository.existsByNicknameAndIdNot(it, 0) }

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
                .toLocalDateTime(),
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
                .toLocalDateTime(),
        )
    }

    fun validateRefreshToken(token: String): Long {
        if (!jwtTokenProvider.validateToken(token)) {
            throw IllegalArgumentException("유효하지 않은 refresh token입니다.")
        }
        return jwtTokenProvider.getUserIdFromToken(token)
    }

    fun refreshToken(member: Member): TokenResponse {
        val now = Date()
        val token = jwtTokenProvider.getToken(member, now)

        return TokenResponse(
            token.first,
            token.second,
            Instant.ofEpochMilli(token.third)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime(),
        )
    }

    private fun loginMember(request: SignInRequest): Member {
        val member = memberRepository.findBySnsIdAndSnsProviderAndEmail(
            request.snsId,
            request.snsProvider,
            request.email,
        ) ?: throw NotFoundException(
            message = "가입된 정보가 없는 유저입니다.",
            code = MessageCode.NOT_FOUND_MEMBER,
        )

        member.deletedAt?.let {
            if (it.plusDays(7) > LocalDateTime.now()) {
                throw MemberException(
                    "탈퇴한 회원입니다.",
                    MessageCode.WITHDRAW_MEMBER,
                )
            }
        }

        return member
    }

    @Transactional
    fun updateUser(member: Member, request: UpdateMemberRequest): Member {
        request.nickname?.let {
            if (it.length > 16) {
                throw IllegalArgumentException(
                    "닉네임은 16자 이하로 입력해주세요.",
                    MessageCode.INVALID_PARAMS,
                )
            }
            if (memberRepository.existsByNicknameAndIdNot(it, member.id)) {
                throw IllegalArgumentException(
                    "이미 존재하는 닉네임입니다.",
                    MessageCode.ALREADY_EXISTS,
                )
            }
        }
        val oldNickname = member.nickname
        member.apply {
            request.nickname?.let { this.nickname = it }
            request.bio?.let { this.bio = it }

            if (request.favoriteRules.isNotEmpty()) {
                this.favoriteRules = request.favoriteRules.joinToString(",")
            }

            if (request.favoriteGenres.isNotEmpty()) {
                this.favoriteGenres = request.favoriteGenres.joinToString(",")
            }

            request.profile?.let {
                val profilePath = s3Service.uploadFile(
                    it.inputStream,
                    it.contentType ?: "image/jpeg",
                    it.originalFilename ?: UUID.randomUUID().toString(),
                    member.id,
                    true,
                )

                this.profile = profilePath
            }

            request.isMarketing?.let {
                if (it && (this.marketingAt == null)) {
                    this.marketingAt = LocalDateTime.now()
                } else if (!it && (this.marketingAt != null)) {
                    this.marketingAt = null
                }
            }
        }

        // CREATOR인 경우 닉네임 변경 시 본인 콘텐츠의 searchText에서 이전 닉네임을 새 닉네임으로 치환
        if (request.nickname != null && member.role == Role.CREATOR && oldNickname != request.nickname) {
            val contents = contentRepository.findByMemberId(member.id)
            if (contents.isNotEmpty()) {
                contents.forEach { content ->
                    content.searchText = content.searchText.replace(oldNickname, request.nickname)
                }
                contentRepository.saveAll(contents)
            }
        }

        return memberRepository.save(member)
    }

    @Transactional
    fun deleteUser(id: Long): Member? = memberRepository.findMemberByIdAndDeletedAtIsNull(id)?.let {
        // 90일간 보관용
        withdrawMemberRepository.save(
            WithdrawMember(
                memberId = it.id,
                snsId = it.snsId,
                snsProvider = it.snsProvider,
                email = it.email,
                nickname = it.nickname,
                bio = it.bio,
            ),
        )
        // 7일간 보관용
        it.deletedAt = LocalDateTime.now()
        it
    }

    private fun makeNickname(): String {
        val (adjustList, nounList) = stringUtil.getWordLists()
        val adjust = adjustList.random()
        val noun = nounList.random()
        val num = (1..9999).random()
        return adjust + noun + num.toString().padStart(4, '0')
    }
}
