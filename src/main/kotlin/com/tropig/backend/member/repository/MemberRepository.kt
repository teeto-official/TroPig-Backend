package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.SnsProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?

    fun existsByEmail(email: String): Boolean

    fun findBySnsIdAndSnsProviderAndEmail(snsId: String, snsProvider: SnsProvider, email: String): Member?
}
