package com.tropig.backend.payment.repository

import com.tropig.backend.payment.entity.CreatorSettlement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CreatorSettlementRepository : JpaRepository<CreatorSettlement, Long> {

    fun findByMemberId(memberId: Long): List<CreatorSettlement>
}


