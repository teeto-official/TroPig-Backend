package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.ContentThumbnail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ContentThumbnailRepository : JpaRepository<ContentThumbnail, Long> {

    fun findByContentIdIn(contentIds: List<Long>): List<ContentThumbnail>

    fun findByContentId(contentId: Long): List<ContentThumbnail>

    fun findTopByContentIdAndCover(contentId: Long, cover: Boolean): ContentThumbnail?

    fun deleteByIdIn(ids: List<Long>)

    @Modifying(clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
            UPDATE content_thumbnail
            SET
                order_no = :orderNo, cover = :isCover
            WHERE
                content_id = :contentId
            AND
                path = :path
        """
    )
    fun updateByContentIdAndPath(
        contentId: Long,
        path: String,
        orderNo: Int,
        isCover: Boolean,
    ): Int
}
