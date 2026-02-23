package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.RelatedContent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RelatedContentRepository: JpaRepository<RelatedContent, Long> {
    fun deleteByContentId(contentId: Long)
}