package com.tropig.backend.payment.repository

import com.tropig.backend.payment.entity.CreatorSettlement
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CreatorSettlementRepository : JpaRepository<CreatorSettlement, Long> {

    fun findByMemberId(memberId: Long): List<CreatorSettlement>

    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): List<CreatorSettlement>

    @Query(
        "SELECT cs FROM CreatorSettlement cs WHERE cs.memberId = :memberId " +
            "AND (cs.createdAt < :cursorCreatedAt OR (cs.createdAt = :cursorCreatedAt AND cs.id < :cursorId)) " +
            "ORDER BY cs.createdAt DESC, cs.id DESC LIMIT :size",
    )
    fun findByMemberIdWithCursor(
        memberId: Long,
        cursorCreatedAt: java.time.LocalDateTime,
        cursorId: Long,
        size: Int,
    ): List<CreatorSettlement>
}
