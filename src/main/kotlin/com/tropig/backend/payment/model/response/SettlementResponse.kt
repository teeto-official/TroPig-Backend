package com.tropig.backend.payment.model.response

import java.time.LocalDateTime

data class SettlementResponse(
    val id: Long,
    val memberId: Long,
    val settlementAmount: Long,
    val status: String,
    val settlementDate: String,
    val memo: String?,
    val createdAt: LocalDateTime,
)
