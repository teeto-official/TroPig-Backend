package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.ContentThumbnail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContentThumbnailRepository : JpaRepository<ContentThumbnail, Long> {

    fun findByContentIdIn(contentIds: List<Long>): List<ContentThumbnail>

    fun findByContentId(contentId: Long): List<ContentThumbnail>

    fun deleteByIdIn(ids: List<Long>)
}
