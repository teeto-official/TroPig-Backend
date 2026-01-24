package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.TermType
import com.tropig.backend.contents.model.result.TagDto
import com.tropig.backend.contents.model.serialize.PublishingInfo
import java.time.LocalDate
import java.time.LocalDateTime

data class ContentDetailResponse(
    val writer: WriterInfo,
    val updatedAt: LocalDateTime,
    val title: String,
    val rule: Rule,
    val genre: Genre,
    val level: Int,
    val playerCountType: PlayerCountType,
    val termType: TermType,
    val tags: List<TagDto>,
    val publishingInfo: List<PublishingInfo>,
    val freeContent: String?,
    val nonFreeContent: String? = null,
)

data class WriterInfo(
    val writerId: Long,
    val nickname: String,
    val profilePath: String?,
)
