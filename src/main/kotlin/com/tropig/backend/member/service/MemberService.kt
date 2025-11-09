package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.util.DateUtil
import com.tropig.backend.common.util.StringUtil
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.model.request.SignInRequest
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.request.UpdateMemberRequest
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.repository.MemberRepository
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
    private val jwtTokenProvider: JwtTokenProvider,
    private val stringUtil: StringUtil,
) {
    fun getUserById(id: Long): Member? {
        return memberRepository.findById(id).orElse(null)
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
}
