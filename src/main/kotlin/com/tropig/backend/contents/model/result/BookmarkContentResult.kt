package com.tropig.backend.contents.model.result

import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import java.time.LocalDateTime

data class BookmarkContentResult(
    val id: Long,
    val alias: String,
    val title: String,
    val ruleId: Long?,
    val genreId: Long?,
    val memberId: Long,
    val playerCountType: PlayerCountType,
    val publishingType: PublishingType?,
    val updatedAt: LocalDateTime,
    val price: Int,
)
