package com.tropig.backend.contents.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.model.result.RelatedContentsResult
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.repository.RelatedContentRepository
import org.springframework.stereotype.Service

@Service
class RelatedContentService(
    private val contentRepository: ContentRepository,
    private val relatedContentRepository: RelatedContentRepository,
) {

    fun getRelatedContents(contentId: Long, isAdult: Boolean): RelatedContentsResult {
        val baseContent = contentRepository.findByIdAndStatusNot(contentId, ContentsStatus.DELETED)
            ?: throw NotFoundException(
                "해당 시나리오/자료를 찾을 수 없습니다.",
                MessageCode.NOT_FOUND_CONTENT,
            )

        return when (baseContent.type) {
            ContentType.SCENARIO -> {
                // 1-1. 관련 시나리오: parent_content_id = content.id
                val scenarioRelations = relatedContentRepository.findByParentContentId(contentId)
                val scenarioIds = scenarioRelations.map { it.contentId }.distinct()

                val relatedScenarios =
                    if (scenarioIds.isEmpty()) {
                        emptyList()
                    } else if (isAdult) {
                        contentRepository.findByIdInAndType(scenarioIds, ContentType.SCENARIO)
                    } else {
                        contentRepository.findContentsByIdInAndTypeAndAdult(
                            scenarioIds,
                            ContentType.SCENARIO,
                            false,
                        )
                    }

                // 1-2. 관련 자료: content_id = content.id 인 RelatedContent에서 parent_content_id 를 자료 ID 로 보고 조회
                val resourceRelations = relatedContentRepository.findByContentId(contentId)
                val resourceIds = resourceRelations.map { it.parentContentId }.distinct()

                val relatedResources =
                    if (resourceIds.isEmpty()) {
                        emptyList()
                    } else if (isAdult) {
                        contentRepository.findByIdInAndType(resourceIds, ContentType.RESOURCE)
                    } else {
                        contentRepository.findContentsByIdInAndTypeAndAdult(
                            resourceIds,
                            ContentType.RESOURCE,
                            false,
                        )
                    }

                RelatedContentsResult(
                    scenarios = relatedScenarios,
                    resources = relatedResources,
                )
            }

            ContentType.RESOURCE -> {
                // 2. 기준 content 가 자료인 경우, 관련 시나리오만 조회 (parent_content_id = 자료 ID)
                val scenarioRelations = relatedContentRepository.findByParentContentId(contentId)
                val scenarioIds = scenarioRelations.map { it.contentId }.distinct()

                val relatedScenarios =
                    if (scenarioIds.isEmpty()) {
                        emptyList()
                    } else if (isAdult) {
                        contentRepository.findByIdInAndType(scenarioIds, ContentType.SCENARIO)
                    } else {
                        contentRepository.findContentsByIdInAndTypeAndAdult(
                            scenarioIds,
                            ContentType.SCENARIO,
                            false,
                        )
                    }

                RelatedContentsResult(
                    scenarios = relatedScenarios,
                    resources = emptyList(),
                )
            }
        }
    }
}
