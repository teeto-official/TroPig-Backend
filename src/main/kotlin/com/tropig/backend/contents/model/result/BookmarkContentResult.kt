package com.tropig.backend.contents.model.result

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import java.time.LocalDateTime

data class BookmarkContentResult(
    val id: Long,
    val alias: String,
    val title: String,
    val rule: Rule,
    val genre: Genre,
    val memberId: Long,
    val playerCountType: PlayerCountType,
    val publishingType: PublishingType,
    val updatedAt: LocalDateTime,
)
