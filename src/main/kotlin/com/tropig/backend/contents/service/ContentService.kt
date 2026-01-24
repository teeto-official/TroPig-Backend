package com.tropig.backend.contents.service

import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.entity.ContentThumbnail
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import com.tropig.backend.contents.model.result.CountSearchContentsResult
import com.tropig.backend.contents.model.result.PickContentResult
import com.tropig.backend.contents.model.result.TagResult
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.repository.ContentTagRepository
import com.tropig.backend.contents.repository.ContentThumbnailRepository
import com.tropig.backend.contents.repository.TagRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class ContentService(
    private val contentRepository: ContentRepository,
    private val contentTagRepository: ContentTagRepository,
    private val contentThumbnailRepository: ContentThumbnailRepository,
) {

    fun findById(id: Long): Content? =
        contentRepository.findById(id).getOrNull()

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

    fun searchContents(
        request: SearchContentRequestDto,
    ): CursorSlice<Content> {
        return contentRepository.searchContents(request)
    }

    fun countSearchContents(
        request: SearchContentRequestDto,
    ): CountSearchContentsResult {
        return contentRepository.countSearchContents(request)
    }

    fun saveAllContentThumbnail(contentThumbnails: List<ContentThumbnail>) =
        contentThumbnailRepository.saveAll(contentThumbnails)

    fun findThumbnailByContentId(contentId: Long): List<ContentThumbnail> =
        contentThumbnailRepository.findByContentId(contentId)

    fun deleteThumbnails(ids: List<Long>) =
        contentThumbnailRepository.deleteByIdIn(ids)

    fun save(content: Content): Content =
        contentRepository.save(content)

    @Transactional
    fun updateContentToDelete(memberId: Long) {
        contentRepository.findByMemberId(memberId).forEach {
            it.status = ContentsStatus.DELETED
        }
    }
}