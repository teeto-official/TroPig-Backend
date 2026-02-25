package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.enums.TermType
import com.tropig.backend.contents.model.result.TagDto
import com.tropig.backend.contents.model.serialize.PublishingInfo
import com.tropig.backend.contents.model.serialize.toPublishingInfoList
import java.time.LocalDateTime

data class CreatorContentDetailResponse(
    val id: Long,
    val type: ContentType,
    val publishedAt: LocalDateTime?,
    val title: String,
    val rule: Rule?,
    val genre: Genre,
    val level: Int?,
    val playerCountType: PlayerCountType,
    val termType: TermType,
    val tags: List<TagDto>,
    val publishingType: PublishingType?,
    val publishingInfo: List<PublishingInfo>,
    val freeContent: String?,
    val nonFreeContent: String? = null,
)

fun Content.toCreatorContentDetailResponse(tags: List<TagDto>, nonFreeContent: String?): CreatorContentDetailResponse =
    CreatorContentDetailResponse(
        id = id,
        type = type,
        publishedAt = publishedAt,
        title = title,
        rule = if (type == ContentType.SCENARIO) rule else null,
        genre = genre,
        level = if (type == ContentType.SCENARIO) level else null,
        playerCountType = playerCountType,
        termType = termType,
        tags = tags,
        publishingType =
        if (type == ContentType.RESOURCE) {
            publishingInfo?.toPublishingInfoList()?.firstOrNull()?.type
        } else {
            null
        },
        publishingInfo = publishingInfo?.toPublishingInfoList() ?: emptyList(),
        freeContent = freeContent,
        nonFreeContent = nonFreeContent,
    )
