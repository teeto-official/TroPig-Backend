package com.tropig.backend.payment.repository

import com.tropig.backend.payment.entity.Purchase
import com.tropig.backend.payment.enums.PurchaseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseRepository : JpaRepository<Purchase, Long>, PurchaseCustomRepository {
    fun findByMemberIdAndContentId(memberId: Long, contentId: Long): Purchase?
    fun existsByMemberIdAndContentIdAndStatus(memberId: Long, contentId: Long, status: PurchaseStatus): Boolean
    fun findByMemberIdAndPaymentId(memberId: Long, paymentId: Long): Purchase?
    fun findByContentIdInAndStatus(contentIds: List<Long>, status: PurchaseStatus): List<Purchase>
}
