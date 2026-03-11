package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.model.result.TagDto
import java.time.LocalDateTime

data class CreatorContentResponse(
    val id: Long,
    val alias: String,
    val title: String,
    val type: ContentType,
    val status: ContentsStatus,
    val rule: Rule,
    val genre: Genre,
    val writer: String,
    val playerCountType: PlayerCountType,
    val thumbnailPath: String?,
    val tags: List<TagDto>,
    val publishedAt: LocalDateTime,
    val freeContent: String?,
)
