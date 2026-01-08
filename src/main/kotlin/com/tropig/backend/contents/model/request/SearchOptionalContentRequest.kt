package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.contents.enums.PlayerCountType
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class SearchOptionalContentRequest(

    @field:Size(max = 3, message = "rules는 최대 3개까지 선택할 수 있습니다.")
    val rules: List<Rule>? = null,

    @field:Size(max = 3, message = "playerCountTypes는 최대 3개까지 선택할 수 있습니다.")
    val playerCountTypes: List<PlayerCountType>? = null,

    @field:Size(max = 3, message = "genres는 최대 3개까지 선택할 수 있습니다.")
    val genres: List<Genre>? = null,

    val sortMode: SortMode,
    val cursorPublishedAt: LocalDateTime? = null,
    val cursorTitle: String? = null,
    val cursorId: Long = 0L,

    val size: Int = 15,
) {
    var isAdult: Boolean = false
}