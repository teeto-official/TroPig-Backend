package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.Rule

data class SimpleSearchContentResponse(
    val id: Long,
    val title: String,
    val rule: Rule,
    val writer: String,
    val thumbnailPath: String?,
)
