package com.tropig.backend.contents.model.request

import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import jakarta.validation.constraints.Size

data class CountSearchContentRequest(
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
    val publishingTypes: List<PublishingType>? = null,
) {
    fun toCountDto(isAdult: Boolean) = SearchContentRequestDto(
        searchText = this.searchText?.replace(" ", ""),
        level = this.level,
        ruleIds = this.rules,
        genreIds = this.genres,
        playerCountTypes = this.playerCountTypes,
        publishingTypes = publishingTypes,
        tags = this.tagIds,
        isAdult = isAdult,
    )
}
