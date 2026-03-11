package com.tropig.backend.contents.model.result

data class TagResult(val tagId: Long, val contentId: Long, val type: String)

data class ContentTagResult(val tagId: Long, val contentId: Long, val type: String, val name: String)

data class TagDto(val tagId: Long, val type: String, val name: String) {
    fun toTagResult(contentId: Long) = TagResult(
        tagId = tagId,
        contentId = contentId,
        type = type,
    )
}
