package com.tropig.backend.contents.model.result

import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.PublishingType

data class PickContentResult(
    val id: Long,
    val title: String,
    val alias: String,
    val thumbnailPath: String?,
    val writerId: Long,
    val tags: List<ContentTagResult>,
    val ruleId: Long?,
    val playerCountType: PlayerCountType,
    val publishingType: PublishingType,
    val publishingInfo: String?,
    val price: Int,
)
