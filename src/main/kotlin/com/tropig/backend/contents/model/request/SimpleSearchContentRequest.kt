package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import java.time.LocalDateTime

data class SimpleSearchContentRequest(
    val searchText: String? = null,
    val sortMode: SortMode,
    val cursorPublishedAt: LocalDateTime? = null,
    val cursorTitle: String? = null,
    val cursorId: Long = 0L,

    val size: Int = 15,
) {
    fun toDto(isAdult: Boolean): SearchContentRequestDto = SearchContentRequestDto(
        searchText = this.searchText,
        isAdult = isAdult,
        type = ContentType.SCENARIO,
        sortMode = this.sortMode,
        cursorPublishedAt = this.cursorPublishedAt,
        cursorTitle = this.cursorTitle,
        cursorId = this.cursorId,
        size = this.size,
    )
}
