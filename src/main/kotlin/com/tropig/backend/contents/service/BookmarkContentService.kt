package com.tropig.backend.contents.service

import com.tropig.backend.contents.model.result.BookmarkContentInfo
import com.tropig.backend.contents.repository.BookmarkContentRepository
import org.springframework.stereotype.Service

@Service
class BookmarkContentService(
    private val bookmarkContentRepository: BookmarkContentRepository,
) {

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
                bookmarked = it.bookmarked
            )
        }.associateBy { it.contentId }
    }
}