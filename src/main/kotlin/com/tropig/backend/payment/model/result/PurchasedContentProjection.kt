package com.tropig.backend.payment.model.result

import java.time.LocalDateTime

data class PurchasedContentProjection(
    val purchaseId: Long,
    val content: PurchasedContentData,
    val purchasedAt: LocalDateTime,
    val purchaseAmount: Long,
)
