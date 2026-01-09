package com.tropig.backend.contents.model.result

data class BookmarkContentInfo(
    val contentId: Long,
    val bookmarkCount: Long,
    val bookmarked: Boolean,
)
