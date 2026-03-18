package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.model.result.TagDto
import java.time.LocalDateTime

data class BookmarkContentResponse(
    val id: Long,
    val alias: String,
    val title: String,
    val rule: Rule,
    val genre: Genre,
    val publishingType: PublishingType?,
    val writer: String,
    val playerCountType: PlayerCountType,
    val thumbnailPath: String?,
    val tags: List<TagDto>,
    val updatedAt: LocalDateTime,
    val price: Int,
)
