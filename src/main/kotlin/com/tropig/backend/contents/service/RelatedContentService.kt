package com.tropig.backend.contents.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.contents.entity.RelatedContent
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.model.result.RelatedContentsResult
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.repository.RelatedContentRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class RelatedContentService(
    private val contentRepository: ContentRepository,
    private val relatedContentRepository: RelatedContentRepository,
) {

    /**
     * 기준 콘텐츠와 연관된 시나리오/자료를 조회합니다.
     *
     * 요구사항:
     * 1. 기준 content의 type이 시나리오인 경우
     *   1-1. 관련 시나리오는 parent_content_id = content.id 로 조회
     *   1-2. 관련 자료는 content_id = content.id 로 조회하고, content.type 이 자료인 것을 조회
     * 2. 기준 content의 type이 자료인 경우, 관련 시나리오만 조회
     * 4. authMember.adult 가 false 이면 content.adult = false 인 것만, true 이면 제한 없음
     */
    fun getRelatedContents(
        contentId: Long,
        isAdult: Boolean,
    ): RelatedContentsResult {
        val baseContent = contentRepository.findByIdAndStatus(contentId, ContentsStatus.PUBLISHED)
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

