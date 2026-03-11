package com.tropig.backend.payment.repository

import com.tropig.backend.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPortonePaymentId(portonePaymentId: String): Payment?
}
