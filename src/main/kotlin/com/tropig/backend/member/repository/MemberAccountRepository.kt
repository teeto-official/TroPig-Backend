package com.tropig.backend.member.repository

import com.tropig.backend.member.entity.MemberAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MemberAccountRepository : JpaRepository<MemberAccount, Long> {
    /**
     * Find account by member ID
     */
    fun findByMemberId(memberId: Long): MemberAccount?

    /**
     * Check if account exists for member
     */
    fun existsByMemberId(memberId: Long): Boolean

    /**
     * Find account by PortOne partner ID
     */
    fun findByPortonePartnerId(partnerId: String): MemberAccount?

    /**
     * Find accounts expiring within specified time range
     */
    fun findByExpiresAtBetween(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<MemberAccount>

    /**
     * Find accounts expiring within 30 days (for renewal notifications)
     */
    @Query("""
        SELECT ma FROM MemberAccount ma
        WHERE ma.expiresAt BETWEEN :now AND :thirtyDaysLater
        ORDER BY ma.expiresAt ASC
    """)
    fun findAccountsNearingExpiry(
        now: LocalDateTime = LocalDateTime.now(),
        thirtyDaysLater: LocalDateTime = LocalDateTime.now().plusDays(30)
    ): List<MemberAccount>

    /**
     * Find expired accounts
     */
    @Query("""
        SELECT ma FROM MemberAccount ma
        WHERE ma.expiresAt < :now
        ORDER BY ma.expiresAt DESC
    """)
    fun findExpiredAccounts(now: LocalDateTime = LocalDateTime.now()): List<MemberAccount>
}
