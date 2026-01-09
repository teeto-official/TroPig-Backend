package com.tropig.backend.contents.service

import com.tropig.backend.contents.entity.ContentTag
import com.tropig.backend.contents.model.result.ContentTagResult
import com.tropig.backend.contents.model.result.TagDto
import com.tropig.backend.contents.model.result.TagResult
import com.tropig.backend.contents.repository.ContentTagRepository
import com.tropig.backend.contents.repository.TagRepository
import org.springframework.stereotype.Service

@Service
class TagService(
    private val contentTagRepository: ContentTagRepository,
) {
    fun findTagNamesByContentIds(contentIds: List<Long>): Map<Long, List<TagDto>> {
        return contentTagRepository.findByContentIdIn(contentIds)
            .map { ContentTagResult(it.tagId, it.contentId, it.type, it.name) }
            .groupBy({ it.contentId }, { TagDto(it.tagId, it.type, it.name) })
    }
}