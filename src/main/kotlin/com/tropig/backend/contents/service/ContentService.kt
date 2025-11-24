package com.tropig.backend.contents.service

import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.repository.ContentTagRepository
import com.tropig.backend.contents.repository.RelatedContentRepository
import com.tropig.backend.contents.repository.TagRepository
import org.springframework.stereotype.Service

@Service
class ContentService(
    private val contentRepository: ContentRepository,
    private val contentTagRepository: ContentTagRepository,
    private val relatedContentRepository: RelatedContentRepository,
    private val tagRepository: TagRepository,
) {
}