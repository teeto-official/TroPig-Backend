package com.tropig.backend.member.service

import com.tropig.backend.common.util.StringUtil
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.model.request.SignUpRequest
import com.tropig.backend.member.model.response.TokenResponse
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.member.service.jwt.JwtTokenProvider
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
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


    fun updateUser(id: Long, email: String?, nickname: String?): Member? {
        val user = memberRepository.findById(id).orElse(null) ?: return null
        
        val updatedUser = user.copy(
            email = email ?: user.email,
            nickname = nickname ?: user.nickname,
        )
        
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
