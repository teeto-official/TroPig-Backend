package com.tropig.backend.payment.model.response

import java.time.LocalDateTime

data class RevenueItemResponse(
    val id: Long,
    val title: String,
    val purchasedAt: LocalDateTime,
    val purchaserNickname: String,
    val amount: Long,
)
