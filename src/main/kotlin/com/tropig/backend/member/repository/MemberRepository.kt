package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.enums.SnsProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface MemberIdNickname {
    val id: Long
    val nickname: String
}

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    @Query("SELECT m.id as id, m.nickname as nickname FROM Member m WHERE m.id IN :ids AND m.deletedAt IS NULL")
    fun findNicknamesByIds(@Param("ids") ids: List<Long>): List<MemberIdNickname>
    fun findByEmail(email: String): Member?

    fun existsByEmail(email: String): Boolean

    fun findBySnsIdAndSnsProviderAndEmail(snsId: String, snsProvider: SnsProvider, email: String): Member?

    fun findByIdInAndRoleAndDeletedAtIsNull(ids: List<Long>, role: Role): List<Member>

    fun findMemberByIdAndDeletedAtIsNull(id: Long): Member?

    fun existsByNicknameAndIdNot(nickname: String, id: Long): Boolean
}
