package com.tropig.backend.payment.model.request

import com.tropig.backend.common.enums.SortMode
import java.time.LocalDateTime

data class PurchasedContentListRequest(
    val sortMode: SortMode,
    val cursorCreatedAt: LocalDateTime? = null,
    val cursorId: Long = 0L,
    val size: Int = 15,
)
