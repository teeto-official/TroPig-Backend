package com.tropig.backend.payment.model.response

import java.time.LocalDateTime

data class RevenueItemResponse(
    val title: String,
    val price: Long,
    val paymentCreatedAt: LocalDateTime,
    val paymentId: Long,
)

