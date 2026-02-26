package com.tropig.backend.contents.model.result

import com.tropig.backend.common.enums.Rule
import com.tropig.backend.contents.enums.PlayerCountType

data class PickContentResult(
    val id: Long,
    val title: String,
    val alias: String,
    val thumbnailPath: String?,
    val writerId: Long,
    val tags: List<ContentTagResult>,
    val rule: Rule,
    val playerCountType: PlayerCountType,
)
