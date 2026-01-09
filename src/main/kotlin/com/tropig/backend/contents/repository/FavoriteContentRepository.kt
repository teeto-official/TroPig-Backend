package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.FavoriteContent
import com.tropig.backend.contents.model.result.projection.FavoriteCountProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FavoriteContentRepository: JpaRepository<FavoriteContent, Long> {

    @Query(
        nativeQuery = true,
        value =
            """
            SELECT
                fc.content_id,
                count(1) as `count`
            FROM
                favorite_content fc
            WHERE
                fc.content_id in (:contentIds)
            AND
                fc.deleted = false
            GROUP BY fc.content_id
        """
    )
    fun countByContentIdsIn(contentIds: List<Long>): List<FavoriteCountProjection>
}