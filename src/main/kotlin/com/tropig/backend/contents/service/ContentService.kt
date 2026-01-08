package com.tropig.backend.contents.service

import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.entity.ContentThumbnail
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.request.SearchContentRequest
import com.tropig.backend.contents.model.request.SearchOptionalContentRequest
import com.tropig.backend.contents.model.result.PickContentResult
import com.tropig.backend.contents.model.result.TagResult
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.repository.ContentTagRepository
import com.tropig.backend.contents.repository.ContentThumbnailRepository
import com.tropig.backend.contents.repository.TagRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ContentService(
    private val contentRepository: ContentRepository,
    private val contentTagRepository: ContentTagRepository,
    private val contentThumbnailRepository: ContentThumbnailRepository,
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
        val thumbnails = getThumbnailPath(contentIds).associateBy { it.contentId }
        val tags = contentTagRepository.findByContentIdIn(contentIds)
            .map { TagResult(it.tagId, it.contentId, it.type) }
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

    fun getThumbnailPath(contentIds: List<Long>): List<ContentThumbnail> {
        return contentThumbnailRepository.findByContentIdIn(contentIds)
    }

    fun searchContents(request: SearchContentRequest, isAdult: Boolean, type: ContentType): CursorSlice<Content> {
        request.isAdult = isAdult
        return contentRepository.searchContents(request, type)
    }

    fun searchOptionalContents(request: SearchOptionalContentRequest, isAdult: Boolean, type: ContentType): CursorSlice<Content> {
        request.isAdult = isAdult
        return contentRepository.searchOptionalContents(request, type)
    }
}