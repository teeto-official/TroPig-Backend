package com.tropig.backend.payment.model.result

import com.tropig.backend.contents.entity.Content
import java.time.LocalDateTime

data class PurchasedContentProjection(
    val content: Content,
    val purchasedAt: LocalDateTime,
    val purchaseAmount: Long,
)
