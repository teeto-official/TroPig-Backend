package com.tropig.backend.contents.model.response

import com.tropig.backend.contents.model.result.TagResult

data class PickContentResponse(
    val title: String,
    val alias: String,
    val thumbnailPath: String?,
    val writer: String,
    val tags: List<TagResult>,
    val orderNo: Int,
)
