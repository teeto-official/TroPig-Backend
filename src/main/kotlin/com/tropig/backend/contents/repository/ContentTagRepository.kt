package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.ContentTag
import com.tropig.backend.contents.model.result.projection.TagResultProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ContentTagRepository: JpaRepository<ContentTag, Long> {

    @Query(
        nativeQuery = true,
        value = """
            SELECT
                t.id as tagId,
                ct.content_id,
                t.type,
                t.name
            FROM
                tag t
            INNER JOIN
                content_tag ct ON t.id = ct.tag_id
            WHERE
                ct.content_id in (:contentIds)
        """
    )
    fun findByContentIdIn(contentIds: List<Long>): List<TagResultProjection>

    @Query(
        nativeQuery = true,
        value = """
            SELECT
                t.id as tagId,
                ct.content_id,
                t.type,
                t.name
            FROM
                tag t
            INNER JOIN
                content_tag ct ON t.id = ct.tag_id
            WHERE
                ct.content_id = :contentId
        """
    )
    fun findByContentId(contentId: Long): List<TagResultProjection>

    fun deleteByContentId(contentId: Long)
}