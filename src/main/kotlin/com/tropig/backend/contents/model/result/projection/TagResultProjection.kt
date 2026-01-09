package com.tropig.backend.contents.model.result.projection

interface TagResultProjection {
    val tagId: Long
    val contentId: Long
    val type: String
    val name: String
}