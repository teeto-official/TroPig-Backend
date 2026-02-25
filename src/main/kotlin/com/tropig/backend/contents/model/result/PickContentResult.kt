package com.tropig.backend.contents.model.result

data class PickContentResult(
    val id: Long,
    val title: String,
    val alias: String,
    val thumbnailPath: String?,
    val writerId: Long,
    val tags: List<TagResult>,
)
