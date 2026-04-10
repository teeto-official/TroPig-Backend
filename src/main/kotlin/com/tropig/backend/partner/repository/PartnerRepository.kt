package com.tropig.backend.partner.repository

import com.tropig.backend.partner.entity.Partner
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PartnerRepository : JpaRepository<Partner, Long> {
    fun findByMemberId(memberId: Long): Partner?
}
