package com.tropig.backend.payment.repository

import com.tropig.backend.payment.entity.Purchase
import com.tropig.backend.payment.enums.PurchaseStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PurchaseRepository : JpaRepository<Purchase, Long>, PurchaseCustomRepository {
    fun findByMemberIdAndContentId(memberId: Long, contentId: Long): Purchase?
    fun existsByMemberIdAndContentIdAndStatus(memberId: Long, contentId: Long, status: PurchaseStatus): Boolean
    fun findByMemberIdAndPaymentId(memberId: Long, paymentId: Long): Purchase?
    fun findByContentIdInAndStatus(contentIds: List<Long>, status: PurchaseStatus): List<Purchase>

    @Query(
        "SELECT p FROM Purchase p WHERE p.contentId IN :contentIds AND p.status = :status " +
            "ORDER BY p.createdAt DESC, p.id DESC"
    )
    fun findByContentIdInAndStatusOrderByCreatedAtDesc(
        contentIds: List<Long>,
        status: PurchaseStatus,
        pageable: Pageable,
    ): List<Purchase>

    @Query(
        "SELECT p FROM Purchase p WHERE p.contentId IN :contentIds AND p.status = :status " +
            "AND (p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
            "ORDER BY p.createdAt DESC, p.id DESC"
    )
    fun findByContentIdInAndStatusWithCursor(
        contentIds: List<Long>,
        status: PurchaseStatus,
        cursorCreatedAt: LocalDateTime,
        cursorId: Long,
        pageable: Pageable,
    ): List<Purchase>
}
