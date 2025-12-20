package com.tropig.backend.contents.model.result

data class TagResult(
    val id: Long,
    val contentId: Long,
    val type: String,
)

interface TagResultProjection {
    val id: Long
    val contentId: Long
    val type: String
}