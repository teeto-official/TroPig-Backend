package com.tropig.backend.contents.model.response

import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.enums.TagType

data class SearchTagResponse(
    val tags: List<TagResponse>,
    val genres: List<GenreResponse>,
    val rules: List<RuleResponse>,
    val publishingType: List<PublishingTypeResponse>,
) {
    data class TagResponse(val tagId: Long, val displayName: String, val type: TagType)

    data class GenreResponse(val id: Long, val displayName: String)

    data class RuleResponse(val id: Long, val displayName: String)

    data class PublishingTypeResponse(val publishingType: PublishingType, val displayName: String)
}
