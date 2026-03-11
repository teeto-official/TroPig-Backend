package com.tropig.backend.payment.model.response

import java.time.LocalDateTime

data class SettlementResponse(
    val id: String, // PortOne transfer ID
    val partnerId: String,
    val memberId: Long,
    val settlementAmount: Long,
    val status: String, // PENDING, COMPLETED, FAILED
    val settlementDate: String,
    val memo: String?,
    val createdAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
)
