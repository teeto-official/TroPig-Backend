package com.tropig.backend.member.service

import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.repository.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class CreatorService(private val memberRepository: MemberRepository) {

    fun getWritersName(writerIds: List<Long>): Map<Long, String> =
        memberRepository.findByIdInAndRoleAndDeletedAtIsNull(writerIds, Role.CREATOR)
            .associate { it.id to it.nickname }

    fun getWriter(writerId: Long): Member? = memberRepository.findByIdOrNull(writerId)
}
