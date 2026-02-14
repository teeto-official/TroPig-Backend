package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.TermType
import jakarta.validation.constraints.*
import java.time.LocalDateTime

data class CreateContentRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    val title: String,

    @field:NotNull(message = "타입은 필수입니다.")
    val type: ContentType,

    @field:NotNull(message = "규칙은 필수입니다.")
    val rule: Rule,

    @field:NotNull(message = "장르는 필수입니다.")
    val genre: Genre,

    @field:NotNull(message = "플레이어 수 타입은 필수입니다.")
    val playerCountType: PlayerCountType,

    @field:NotNull(message = "기간 타입은 필수입니다.")
    val termType: TermType,

    val publishingInfo: String? = null,

    @field:NotNull(message = "상태는 필수입니다.")
    val status: ContentsStatus,

    @field:NotNull(message = "성인 여부는 필수입니다.")
    val adult: Boolean,

    val publishedAt: LocalDateTime? = null,

    val freeContent: String? = null,

    val nonFreeContent: String? = null,

    @field:NotNull(message = "가격은 필수입니다.")
    @field:Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    val price: Double,

    @field:NotNull(message = "레벨은 필수입니다.")
    @field:Min(value = 1, message = "레벨은 1 이상이어야 합니다.")
    @field:Max(value = 4, message = "레벨은 4 이하여야 합니다.")
    val level: Int,

    @field:Size(max = 8, message = "태그는 최대 8개까지 선택할 수 있습니다.")
    val tagIds: List<Long>? = null,

    val relatedContentIds: List<Long>? = null,
)
