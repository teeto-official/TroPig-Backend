package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.contents.enums.PlayerCountType

data class SearchContentRequest(
    val searchText: String,
    val rule: Rule? = null,
    val level: Int? = null,
    val playerCountType: PlayerCountType? = null,
    val genre: Genre? = null,

    val sortMode: SortMode,
    val cursorPublishedAt: Long? = null,
    val cursorTitle: String? = null,
    val cursorId: Long = 0L,

    val size: Int = 15,
) {
    var isAdult: Boolean = false
}
