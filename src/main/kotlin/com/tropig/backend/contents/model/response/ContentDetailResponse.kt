package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.TermType
import com.tropig.backend.contents.model.result.TagDto
import com.tropig.backend.contents.model.serialize.PublishingInfo
import com.tropig.backend.contents.model.serialize.toPublishingInfoList
import com.tropig.backend.member.entity.Member
import java.time.LocalDate
import java.time.LocalDateTime

data class ContentDetailResponse(
    val type: ContentType,
    val writer: WriterInfo,
    val publishedAt: LocalDateTime?,
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
    val isBookmark: Boolean,
)

fun Content.toDetailResponse(
    writer: Member?,
    tags: List<TagDto>,
    purchasedContent: String?,
    isBookmark: Boolean,
): ContentDetailResponse {
    val writerInfo = writer?.let {
        it.deletedAt?.let {
            WriterInfo(writer.id)
        } ?: run {
            WriterInfo(
                writerId = writer.id,
                nickname = writer.nickname,
                profilePath = writer.profile
            )
        }
    } ?: run {
        WriterInfo()
    }
    return ContentDetailResponse(
        type = type,
        writer = writerInfo,
        publishedAt = publishedAt,
        title = title,
        rule = rule,
        genre = genre,
        level = level,
        playerCountType = playerCountType,
        termType = termType,
        tags = tags,
        publishingInfo = publishingInfo?.toPublishingInfoList() ?: emptyList(),
        freeContent = freeContent,
        nonFreeContent = purchasedContent,
        isBookmark = isBookmark,
    )
}

data class WriterInfo(
    val writerId: Long = 0L,
    val nickname: String = "탈퇴한 유저",
    val profilePath: String? = null,
)
