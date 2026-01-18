package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.util.DateUtil
import com.tropig.backend.common.util.StringUtil
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.entity.WithdrawMember
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.TokenResponse
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
    private val withdrawMemberRepository: WithdrawMemberRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val stringUtil: StringUtil,
) {

    companion object {
        private const val REJOIN_DAYS: Long = 7
    }

    fun getUserByEmail(email: String): Member? {
        return memberRepository.findByEmail(email)
    }

    @Transactional
    fun createMember(request: SignUpRequest): Member {
        memberRepository.findByEmail(request.email)?.let {
            it.deletedAt?.let { date ->
                if (date.plusDays(REJOIN_DAYS) >= LocalDateTime.now()) {
                    // 탈퇴일로부터 7일이 지나지 않음
                    throw IllegalArgumentException("탈퇴 후, ${REJOIN_DAYS}일간 재가입이 불가합니다. 탈퇴일: $date")
                }
            } ?: throw IllegalArgumentException("해당 이메일을 사용하는 유저가 있습니다. email: ${request.email}")
        }

        val nickname = generateSequence { makeNickname() }
            .first { !memberRepository.existsByNickname(it) }

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


    @Transactional
    fun updateUser(member: Member, request: UpdateMemberRequest): Member {
        request.nickname?.let {
            if (memberRepository.existsByNickname(it)) {
                throw IllegalArgumentException("이미 존재하는 닉네임입니다.")
            }
        }
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

            request.isMarketing?.let {
                if (it && (this.marketingAt == null)) {
                    this.marketingAt = LocalDateTime.now()
                } else if (!it && (this.marketingAt != null)) {
                    this.marketingAt = null
                }
            }

        }
        
        return memberRepository.save(updatedUser)
    }

    fun getWritersName(writerIds: List<Long>): Map<Long, String> {
        return memberRepository.findByIdInAndRoleAndDeletedAtIsNull(writerIds, Role.CREATOR)
            .associate { it.id to it.nickname }
    }

    @Transactional
    fun deleteUser(id: Long): Member? {
        return memberRepository.findMemberByIdAndDeletedAtIsNull(id)?.let {
            // 90일간 보관용
            withdrawMemberRepository.save(
                WithdrawMember(
                    memberId = it.id,
                    snsId = it.snsId,
                    snsProvider = it.snsProvider,
                    email = it.email,
                    nickname = it.nickname,
                    bio = it.bio,
                )
            )
            // 7일간 보관용
            it.deletedAt = LocalDateTime.now()
            it
        }
    }

    private fun makeNickname(): String {
        val (adjustList, nounList) = stringUtil.getWordLists()
        val adjust = adjustList.random()
        val noun = nounList.random()
        val num = (1..9999).random()
        return adjust + noun + num.toString().padStart(4, '0')
    }
}
