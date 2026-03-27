package com.tropig.backend.contents.model.result

import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import java.time.LocalDateTime

data class SearchContentResult(
    val id: Long,
    val alias: String,
    val title: String,
    val type: ContentType,
    val memberId: Long,
    val ruleId: Long?,
    val genreId: Long?,
    val playerCountType: PlayerCountType,
    val adult: Boolean,
    val publishedAt: LocalDateTime,
    val price: Double,
    val level: Int,
    val likeCount: Int,
    val bookmarkCount: Int,
    val tagList: List<String>,
)
