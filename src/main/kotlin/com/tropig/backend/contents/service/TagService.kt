package com.tropig.backend.contents.service

import com.tropig.backend.contents.entity.Tag
import com.tropig.backend.contents.model.result.ContentTagResult
import com.tropig.backend.contents.model.result.TagDto
import com.tropig.backend.contents.repository.ContentTagRepository
import com.tropig.backend.contents.repository.TagRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class TagService(private val contentTagRepository: ContentTagRepository, private val tagRepository: TagRepository) {
    fun findTagNamesByContentIds(contentIds: List<Long>): Map<Long, List<TagDto>> =
        contentTagRepository.findByContentIdIn(contentIds)
            .map { ContentTagResult(it.tagId, it.contentId, it.type, it.name) }
            .groupBy({ it.contentId }, { TagDto(it.tagId, it.type, it.name) })

    @Cacheable(cacheNames = ["tagList"])
    fun findAllTags(): List<Tag> = tagRepository.findAll()

    @Cacheable(cacheNames = ["contentTags"], key = "#contentId")
    fun findByContentId(contentId: Long): List<TagDto> = contentTagRepository.findByContentId(contentId)
        .map { TagDto(it.tagId, it.type, it.name) }
}
