package com.tropig.backend.payment.repository

import com.tropig.backend.payment.entity.Purchase
import com.tropig.backend.payment.enums.PurchaseStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PurchaseRepository : JpaRepository<Purchase, Long> {
    fun findByMemberIdAndContentId(memberId: Long, contentId: Long): Purchase?
    fun findByMemberIdAndStatus(memberId: Long, status: PurchaseStatus): List<Purchase>
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<Purchase>
    fun existsByMemberIdAndContentIdAndStatus(memberId: Long, contentId: Long, status: PurchaseStatus): Boolean
    fun findByMemberIdAndPaymentId(memberId: Long, paymentId: Long): Purchase?
}
