package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.BookmarkContent
import com.tropig.backend.contents.model.result.BookmarkContentInfo
import com.tropig.backend.contents.model.result.projection.BookmarkContentProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BookmarkContentRepository: JpaRepository<BookmarkContent, Long> {

    @Query(
        nativeQuery = true,
        value =
        """
            SELECT
                bc.content_id,
                count(1) as bookmarkCount,
                SUM(
                    CASE
                        WHEN bc.member_id = :memberId THEN 1
                        ELSE 0
                    END
                ) > 0 AS bookmarked
            FROM
                bookmark_content bc
            WHERE
                bc.content_id in (:contentIds)
            AND
                bc.deleted = false
            GROUP BY bc.content_id
        """
    )
    fun getBookmarkInfoByContentIdsAndMemberId(contentIds: List<Long>, memberId: Long): List<BookmarkContentProjection>

    @Query(
        nativeQuery = true,
        value =
            """
            SELECT
                bc.content_id,
                count(1) as bookmarkCount,
                false as bookmarked
            FROM
                bookmark_content bc
            WHERE
                bc.content_id in (:contentIds)
            AND
                bc.deleted = false
            GROUP BY bc.content_id
        """
    )
    fun getBookmarkInfoByContentIds(contentIds: List<Long>): List<BookmarkContentProjection>
}