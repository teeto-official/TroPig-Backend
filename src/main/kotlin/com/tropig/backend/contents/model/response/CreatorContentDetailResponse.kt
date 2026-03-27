package com.tropig.backend.contents.model.response

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
    val writer: WriterInfo,
    val publishedAt: LocalDateTime?,
    val title: String,
    val ruleId: Long?,
    val genreId: Long?,
    val level: Int?,
    val playerCountType: PlayerCountType,
    val termType: TermType,
    val tags: List<TagDto>,
    val publishingType: PublishingType?,
    val publishingInfo: List<PublishingInfo>,
    val freeContent: String?,
    val nonFreeContent: String? = null,
    val thumbnailPath: String? = null,
    val price: Int,
    val preventRightClick: Boolean = false,
)

fun Content.toCreatorContentDetailResponse(
    tags: List<TagDto>,
    nonFreeContent: String?,
    thumbnailPath: String?,
    writer: WriterInfo,
): CreatorContentDetailResponse = CreatorContentDetailResponse(
    id = id,
    type = type,
    writer = writer,
    publishedAt = publishedAt,
    title = title,
    ruleId = if (type == ContentType.SCENARIO) ruleId else null,
    genreId = genreId,
    level = if (type == ContentType.SCENARIO) level else null,
    playerCountType = playerCountType,
    termType = termType,
    tags = tags,
    publishingType = if (type == ContentType.RESOURCE) publishingType else null,
    publishingInfo = publishingInfo?.toPublishingInfoList() ?: emptyList(),
    freeContent = freeContent,
    nonFreeContent = nonFreeContent,
    thumbnailPath = thumbnailPath,
    price = price.toInt(),
    preventRightClick = preventRightClick,
)
