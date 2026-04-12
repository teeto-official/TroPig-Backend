package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class SearchContentRequest(
    val searchText: String? = null,
    val level: List<Int>? = null,
    @field:Size(max = 3, message = "rules는 최대 3개까지 선택할 수 있습니다.")
    val rules: List<Long>? = null,
    @field:Size(max = 3, message = "genres는 최대 3개까지 선택할 수 있습니다.")
    val genres: List<Long>? = null,
    @field:Size(max = 3, message = "playerCountTypes는 최대 3개까지 선택할 수 있습니다.")
    val playerCountTypes: List<PlayerCountType>? = null,
    @field:Size(max = 8, message = "tagIds는 최대 8개까지 선택할 수 있습니다.")
    val tagIds: List<Long>? = null,
    @field:Size(max = 8, message = "publishingTypes는 최대 8개까지 선택할 수 있습니다.")
    val publishingTypes: List<PublishingType>? = null,

    val sortMode: SortMode,
    val cursorPublishedAt: LocalDateTime? = null,
    val cursorTitle: String? = null,
    val cursorId: Long = 0L,

    val size: Int = 15,
) {
    fun toDto(isAdult: Boolean, type: ContentType): SearchContentRequestDto =
        SearchContentRequestDto(
            searchText = this.searchText?.replace(" ", ""),
            level = this.level,
            ruleIds = this.rules,
            genreIds = this.genres,
            playerCountTypes = this.playerCountTypes,
            tags = this.tagIds,
            isAdult = isAdult,
            type = type,
            publishingTypes = this.publishingTypes,
            sortMode = this.sortMode,
            cursorPublishedAt = this.cursorPublishedAt,
            cursorTitle = this.cursorTitle,
            cursorId = this.cursorId,
            size = this.size,
        )
}
