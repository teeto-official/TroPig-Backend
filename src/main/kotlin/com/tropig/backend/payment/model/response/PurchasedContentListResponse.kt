package com.tropig.backend.payment.model.response

import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.model.result.TagDto
import java.time.LocalDateTime

data class PurchasedContentListResponse(
    val id: Long,
    val alias: String,
    val title: String,
    val type: ContentType,
    val ruleId: Long?,
    val genreId: Long?,
    val writer: String,
    val playerCountType: PlayerCountType,
    val thumbnailPath: String?,
    val tags: List<TagDto>,
    val isBookmark: Boolean,
    val publishedAt: LocalDateTime,
    val freeContent: String?,
    val purchasedAt: LocalDateTime,
    val purchaseAmount: Long,
    val price: Int,
    val publishingType: PublishingType?,
)
