package com.tropig.backend.contents.repository

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.result.BookmarkContentResult
import java.time.LocalDateTime

interface BookmarkContentCustomRepository {

    fun getBookmarkList(
        memberId: Long,
        type: ContentType,
        cursorId: Long?,
        cursorCreatedAt: LocalDateTime?,
        sortMode: SortMode,
        size: Int,
    ): CursorSlice<BookmarkContentResult>
}