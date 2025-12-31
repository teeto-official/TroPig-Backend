package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.MemberAuthInfo
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MemberAuthInfoRepository: JpaRepository<MemberAuthInfo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MemberAuthInfo m where m.memberId = :memberId")
    fun findByMemberIdForUpdate(memberId: Long): MemberAuthInfo?

    fun findByMemberId(memberId: Long): MemberAuthInfo?
}

