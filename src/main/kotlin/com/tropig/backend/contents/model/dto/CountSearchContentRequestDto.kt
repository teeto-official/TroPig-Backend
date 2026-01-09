package com.tropig.backend.contents.model.dto

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.PlayerCountType
import java.time.LocalDateTime

data class CountSearchContentRequestDto(
    val searchText: String? = null,
    val level: List<Int>? = null,
    val rules: List<Rule>? = null,
    val genres: List<Genre>? = null,
    val playerCountTypes: List<PlayerCountType>? = null,
    val tags: List<Long>? = null,
    val isAdult: Boolean,
)