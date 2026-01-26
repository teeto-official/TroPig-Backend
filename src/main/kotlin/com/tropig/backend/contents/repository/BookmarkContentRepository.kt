package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.BookmarkContent
import com.tropig.backend.contents.model.result.projection.BookmarkContentProjection
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Bool

@Repository
interface BookmarkContentRepository: JpaRepository<BookmarkContent, Long>, BookmarkContentCustomRepository {

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

    @Modifying
    @Query(
        """
    UPDATE BookmarkContent b
    SET b.deleted = true, b.updatedAt = CURRENT_TIMESTAMP
    WHERE b.memberId = :memberId
      AND b.contentId = :contentId
      AND b.deleted = false
    """
    )
    fun deleteBookmark(memberId: Long, contentId: Long): Int

    @Modifying
    @Query(
        value = """
        INSERT INTO bookmark_content (member_id, content_id, deleted, created_at, updated_at)
        VALUES (:memberId, :contentId, false, now(), now())
        ON CONFLICT (member_id, content_id)
        DO UPDATE SET
            deleted = false,
            updated_at = now()
    """,
        nativeQuery = true
    )
    fun upsertBookmark(memberId: Long, contentId: Long): Int

    fun existsByMemberIdAndContentIdAndDeleted(memberId: Long, contentId: Long, deleted: Boolean): Boolean
}