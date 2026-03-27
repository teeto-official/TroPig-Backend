package com.tropig.backend.contents.model.response

data class SimpleSearchContentResponse(
    val id: Long,
    val title: String,
    val ruleId: Long?,
    val writer: String,
    val thumbnailPath: String?,
)
