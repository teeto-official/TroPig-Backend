package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.TagType

data class SearchTagResponse(
    val tags: List<TagResponse>,
    val genres: List<GenreResponse>,
    val rules: List<RuleResponse>
) {
    data class TagResponse(
        val tagId: Long,
        val displayName: String,
        val type: TagType
    )

    data class GenreResponse(
        val genre: Genre,
        val displayName: String
    )

    data class RuleResponse(
        val rule: Rule,
        val displayName: String,
    )
}
