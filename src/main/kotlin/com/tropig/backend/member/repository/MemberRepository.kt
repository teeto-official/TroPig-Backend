package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.enums.SnsProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmailHash(emailHash: String): Member?

    fun existsByEmailHash(emailHash: String): Boolean

    fun findBySnsIdAndSnsProviderAndEmailHash(snsId: String, snsProvider: SnsProvider, emailHash: String): Member?

    fun findByIdInAndRoleAndDeletedAtIsNull(ids: List<Long>, role: Role): List<Member>

    fun findMemberByIdAndDeletedAtIsNull(id: Long): Member?

    fun existsByNicknameAndIdNot(nickname: String, id: Long): Boolean
}
