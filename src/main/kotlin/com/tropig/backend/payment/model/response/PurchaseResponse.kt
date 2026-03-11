package com.tropig.backend.payment.model.response

import com.tropig.backend.payment.enums.PaymentStatus
import com.tropig.backend.payment.enums.PurchaseStatus
import java.time.LocalDateTime

data class PurchaseResponse(
    val id: Long,
    val memberId: Long,
    val contentId: Long,
    val paymentId: Long,
    val amount: Long,
    val status: PurchaseStatus,
    val paymentStatus: PaymentStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
