package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.model.result.ContentTagResult

data class PickContentResponse(
    val id: Long,
    val title: String,
    val alias: String,
    val thumbnailPath: String?,
    val writer: String,
    val tags: List<ContentTagResult>,
    val rule: Rule,
    val playerCountType: PlayerCountType,
    val isBookmark: Boolean,
    val orderNo: Int,
)
