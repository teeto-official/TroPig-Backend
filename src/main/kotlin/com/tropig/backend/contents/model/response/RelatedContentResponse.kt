package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.model.result.TagDto

data class RelatedContentItemResponse(
    val id: Long,
    val title: String,
    val alias: String,
    val rule: Rule,
    val playerCountType: PlayerCountType,
    val thumbnailPath: String?,
    val tags: List<TagDto>,
    val writer: String,
    val price: Int,
)

data class RelatedContentsResponse(
    val scenarios: List<RelatedContentItemResponse>,
    val resources: List<RelatedContentItemResponse>,
)
