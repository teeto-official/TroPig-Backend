package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.model.dto.CountSearchContentRequestDto
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import jakarta.validation.constraints.Size

data class CountSearchContentRequest(
    val searchText: String? = null,
    val level: List<Int>? = null,
    @field:Size(max = 3, message = "rules는 최대 3개까지 선택할 수 있습니다.")
    val rules: List<Rule>? = null,
    @field:Size(max = 3, message = "genres는 최대 3개까지 선택할 수 있습니다.")
    val genres: List<Genre>? = null,
    @field:Size(max = 3, message = "playerCountTypes는 최대 3개까지 선택할 수 있습니다.")
    val playerCountTypes: List<PlayerCountType>? = null,
    @field:Size(max = 8, message = "tags는 최대 8개까지 선택할 수 있습니다.")
    val tags: List<String>? = null,
) {
    fun toCountDto(
        isAdult: Boolean,
        tagIds: List<Long>?,
    ) = SearchContentRequestDto(
        searchText = this.searchText,
        level = this.level,
        rules = this.rules,
        genres = this.genres,
        playerCountTypes = this.playerCountTypes,
        tags = tagIds,
        isAdult = isAdult,
    )
}