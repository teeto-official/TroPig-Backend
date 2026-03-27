package com.tropig.backend.payment.model.result

import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import java.time.LocalDateTime

data class PurchasedContentData(
    val id: Long,
    val alias: String,
    val title: String,
    val type: ContentType,
    val ruleId: Long?,
    val genreId: Long?,
    val memberId: Long,
    val playerCountType: PlayerCountType,
    val publishingType: PublishingType,
    val publishedAt: LocalDateTime?,
    val freeContent: String?,
    val price: Double,
)
