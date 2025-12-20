package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.ContentTag
import com.tropig.backend.contents.entity.Tag
import com.tropig.backend.contents.model.result.TagResult
import com.tropig.backend.contents.model.result.TagResultProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ContentTagRepository: JpaRepository<ContentTag, Long> {

    @Query(
        """
            SELECT
                t.id,
                ct.contentId,
                t.type
            FROM
                Tag t
            INNER JOIN
                ContentTag ct ON t.id = ct.tagId
            WHERE
                ct.contentId in (:contentIds)
        """
    )
    fun findByContentIdIn(contentIds: List<Long>): List<TagResultProjection>
}