package com.tropig.backend.contents.service

import com.tropig.backend.common.enums.SortMode
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.result.BookmarkContentInfo
import com.tropig.backend.contents.model.result.BookmarkContentResult
import com.tropig.backend.contents.repository.BookmarkContentRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BookmarkContentService(private val bookmarkContentRepository: BookmarkContentRepository) {

    fun getBookmarkInfo(memberId: Long?, contentIds: List<Long>): Map<Long, BookmarkContentInfo> {
        val bookmarkInfo = memberId?.let {
            bookmarkContentRepository.getBookmarkInfoByContentIdsAndMemberId(contentIds, memberId)
        } ?: run {
            bookmarkContentRepository.getBookmarkInfoByContentIds(contentIds)
        }

        return bookmarkInfo.map {
            BookmarkContentInfo(
                contentId = it.contentId,
                bookmarkCount = it.bookmarkCount,
                bookmarked = it.bookmarked,
            )
        }.associateBy { it.contentId }
    }

    fun getBookmarkList(
        memberId: Long,
        type: ContentType,
        cursorId: Long?,
        cursorCreatedAt: LocalDateTime?,
        sortMode: SortMode,
        size: Int,
    ): CursorSlice<BookmarkContentResult> = bookmarkContentRepository.getBookmarkList(
        memberId,
        type,
        cursorId,
        cursorCreatedAt,
        sortMode,
        size,
    )

    @Transactional
    fun saveBookmark(memberId: Long, contentId: Long) {
        bookmarkContentRepository.upsertBookmark(memberId, contentId)
    }

    @Transactional
    fun deleteBookmark(memberId: Long, contentId: Long) {
        bookmarkContentRepository.deleteBookmark(memberId, contentId)
    }

    fun existsBookmark(memberId: Long, contentId: Long): Boolean =
        bookmarkContentRepository.existsByMemberIdAndContentIdAndDeleted(memberId, contentId, false)
}
