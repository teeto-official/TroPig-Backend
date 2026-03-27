package com.tropig.backend.payment.model.request

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.contents.enums.ContentType
import java.time.LocalDateTime

data class PurchasedContentListRequest(
    val type: ContentType?,
    val sortMode: SortMode,
    val cursorCreatedAt: LocalDateTime? = null,
    val cursorId: Long = 0L,
    val size: Int = 15,
)
