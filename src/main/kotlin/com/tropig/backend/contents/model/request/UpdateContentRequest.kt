package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.*
import com.tropig.backend.contents.model.serialize.PublishingInfo
import jakarta.validation.constraints.*
import java.time.LocalDateTime

data class UpdateContentRequest(
    val title: String?,
    @field:NotNull(message = "타입은 필수입니다.")
    val type: ContentType,
    val rule: Rule?,
    val genre: Genre?,
    val playerCountType: PlayerCountType?,
    val termType: TermType?,
    val publishingType: PublishingType? = null,
    val publishingInfo: List<PublishingInfo>? = null,

    @field:NotNull(message = "상태는 필수입니다.")
    val status: ContentsStatus,
    val adult: Boolean = false,
    val publishedAt: LocalDateTime? = null,
    val freeContent: String? = null,
    val nonFreeContent: String? = null,

    @field:Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    val price: Double = 0.0,

    @field:Min(value = 1, message = "레벨은 1 이상이어야 합니다.")
    @field:Max(value = 4, message = "레벨은 4 이하여야 합니다.")
    val level: Int? = null,

    @field:Size(max = 8, message = "태그는 최대 8개까지 선택할 수 있습니다.")
    val tagIds: List<Long>? = null,

    @field:Size(max = 10, message = "연관 시나리오는 최대 10개까지 등록할 수 있습니다.")
    val relatedContentIds: List<Long>? = null,

    val thumbnails: List<ContentThumbnailInfo>? = null,
)

data class ContentThumbnailInfo(val path: String, val orderNo: Int, val isCover: Boolean)
