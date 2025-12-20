package com.tropig.backend.contents.service

import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.result.PickContentResult
import com.tropig.backend.contents.model.result.TagResult
import com.tropig.backend.contents.repository.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ContentService(
    private val contentRepository: ContentRepository,
    private val contentTagRepository: ContentTagRepository,
    private val contentThumbnailRepository: ContentThumbnailRepository,
    private val relatedContentRepository: RelatedContentRepository,
    private val tagRepository: TagRepository,
) {

    fun findByIdInAndType(ids: List<Long>, type: ContentType): List<Content> =
        contentRepository.findByIdInAndType(ids, type)

    @Cacheable(value = ["pickContentByType"], key = "#type.name() + '_' + #isAdult")
    fun getPickContentsByType(type: ContentType, isAdult: Boolean, contentIds: List<Long>): List<PickContentResult> {
        val contents = if (isAdult) {
            contentRepository.findByIdInAndType(contentIds, type)
        } else {
            contentRepository.findContentsByIdInAndTypeAndAdult(contentIds, type, false)
        }
        val thumbnails = contentThumbnailRepository.findByContentIdIn(contentIds)
            .associateBy { it.contentId }
        val tags = contentTagRepository.findByContentIdIn(contentIds)
            .map { TagResult(it.id, it.contentId, it.type) }
            .groupBy { it.contentId }

        return contents.map {
            PickContentResult(
                id = it.id,
                title = it.title,
                alias = it.alias,
                thumbnailPath = thumbnails[it.id]?.path,
                writerId = it.memberId,
                tags = tags[it.id] ?: emptyList()
            )
        }
    }
}