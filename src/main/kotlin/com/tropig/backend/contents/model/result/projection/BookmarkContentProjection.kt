package com.tropig.backend.contents.model.result.projection

interface BookmarkContentProjection {
    val contentId: Long
    val bookmarkCount: Long
    val bookmarked: Boolean
}
