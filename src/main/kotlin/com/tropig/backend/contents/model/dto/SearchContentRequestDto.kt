package com.tropig.backend.contents.model.dto

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType
import java.time.LocalDateTime

data class SearchContentRequestDto(
    val searchText: String? = null,
    val level: List<Int>? = null,
    val rules: List<Rule>? = null,
    val genres: List<Genre>? = null,
    val playerCountTypes: List<PlayerCountType>? = null,
    val tags: List<Long>? = null,
    val publishingTypes: List<PublishingType>? = null,

    val isAdult: Boolean,
    val type: ContentType = ContentType.SCENARIO,
    val sortMode: SortMode = SortMode.LATEST,
    val cursorPublishedAt: LocalDateTime? = null,
    val cursorTitle: String? = null,
    val cursorId: Long = 0L,

    val size: Int = 15,
)
