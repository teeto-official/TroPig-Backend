package com.tropig.backend.common.model

import com.tropig.backend.contents.model.result.BookmarkContentInfo
import com.tropig.backend.contents.model.result.TagDto

data class SearchContext(
    val nickByMemberId: Map<Long, String> = emptyMap(),
    val tagsByContentId: Map<Long, List<TagDto>> = emptyMap(),
    val bookmarkInfo: Map<Long, BookmarkContentInfo>,
    val favoriteCounts: Map<Long, Long>,
    val thumbnailPaths: Map<Long, String?>,
)
