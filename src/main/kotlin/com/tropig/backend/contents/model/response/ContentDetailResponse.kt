package com.tropig.backend.contents.model.response

import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.enums.TermType
import com.tropig.backend.contents.model.result.TagDto
import com.tropig.backend.contents.model.serialize.PublishingInfo
import com.tropig.backend.contents.model.serialize.toPublishingInfoList
import com.tropig.backend.member.entity.Member
import java.time.LocalDateTime

data class ContentDetailResponse(
    val id: Long,
    val alias: String,
    val type: ContentType,
    val writer: WriterInfo,
    val thumbnailPath: String?,
    val publishedAt: LocalDateTime?,
    val title: String,
    val ruleId: Long?,
    val genreId: Long?,
    val level: Int,
    val playerCountType: PlayerCountType,
    val termType: TermType,
    val tags: List<TagDto>,
    val publishingType: PublishingType,
    val publishingInfo: List<PublishingInfo>,
    val freeContent: String?,
    val nonFreeContent: String? = null,
    val isBookmark: Boolean,
    val price: Double,
    val preventRightClick: Boolean = false,
)

fun Content.toDetailResponse(
    writer: Member?,
    tags: List<TagDto>,
    purchasedContent: String?,
    isBookmark: Boolean,
    thumbnailPath: String? = null,
    writerProfileUrl: String? = null,
): ContentDetailResponse {
    val writerInfo = writer?.let {
        it.deletedAt?.let {
            WriterInfo(writer.id)
        } ?: run {
            WriterInfo(
                writerId = writer.id,
                nickname = writer.nickname,
                profilePath = writerProfileUrl,
            )
        }
    } ?: run {
        WriterInfo()
    }
    return ContentDetailResponse(
        id = id,
        alias = alias,
        type = type,
        writer = writerInfo,
        thumbnailPath = thumbnailPath,
        publishedAt = publishedAt,
        title = title,
        ruleId = ruleId,
        genreId = genreId,
        level = level,
        playerCountType = playerCountType,
        termType = termType,
        tags = tags,
        publishingType = publishingType,
        publishingInfo = publishingInfo?.toPublishingInfoList() ?: emptyList(),
        freeContent = freeContent,
        nonFreeContent = purchasedContent,
        isBookmark = isBookmark,
        price = price,
        preventRightClick = preventRightClick,
    )
}

data class WriterInfo(val writerId: Long = 0L, val nickname: String = "탈퇴한 유저", val profilePath: String? = null)
