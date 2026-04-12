package com.tropig.backend.contents.model.response

data class AdminPickContentDetailResponse(
    val id: Long,
    val title: String,
    val alias: String,
    val thumbnailPath: String?,
    val writer: String,
    val type: String,
    val orderNo: Int,
)
