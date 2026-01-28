package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.model.response.RelatedContentItemResponse
import com.tropig.backend.contents.model.response.RelatedContentsResponse
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.RelatedContentService
import com.tropig.backend.contents.service.TagService
import com.tropig.backend.member.service.CreatorService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/api/content")
class RelatedContentController(
    private val relatedContentService: RelatedContentService,
    private val creatorService: CreatorService,
    private val tagService: TagService,
    private val contentService: ContentService,
) {

    /**
     * 기준 콘텐츠와 연관된 시나리오/자료를 조회합니다.
     *
     * 1. 기준 content 의 type 이 시나리오인 경우
     *   1-1. 관련 시나리오는 parent_content_id = content.id 로 조회
     *   1-2. 관련 자료는 content_id = content.id 로 조회하고, content.type 이 자료인 것을 조회
     * 2. 기준 content 의 type 이 자료인 경우, 관련 시나리오만 조회
     * 3. Response: 썸네일 path, content.id, content.title, content.alias,
     *    content.rule, tag 리스트, creator.name, content.player_count
     * 4. authMember.adult = false 이면 content.adult = false 인 것만,
     *    true 이면 content.adult 와 상관없이 조회
     */
    @GetMapping("/related/{contentId}")
    fun getRelatedContents(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember?,
        @PathVariable contentId: Long,
    ): RelatedContentsResponse {
        val isAdult = authMember?.adult ?: false

        val relatedContents = relatedContentService.getRelatedContents(contentId, isAdult)
        val allContents = relatedContents.scenarios + relatedContents.resources
        if (allContents.isEmpty()) {
            return RelatedContentsResponse(
                scenarios = emptyList(),
                resources = emptyList(),
            )
        }

        val contentIds = allContents.map { it.id }
        val writerIds = allContents.map { it.memberId }.distinct()

        val writerNames = creatorService.getWritersName(writerIds)
        val tagsByContentId = tagService.findTagNamesByContentIds(contentIds)
        val thumbnailPaths = contentService.getThumbnailPath(contentIds)
            .associateBy({ it.contentId }, { it.path })

        fun mapToItem(content: Content): RelatedContentItemResponse {
            return RelatedContentItemResponse(
                id = content.id,
                title = content.title,
                alias = content.alias,
                rule = content.rule,
                playerCountType = content.playerCountType,
                thumbnailPath = thumbnailPaths[content.id],
                tags = tagsByContentId[content.id].orEmpty(),
                writer = writerNames[content.memberId] ?: "탈퇴한 작가입니다.",
            )
        }

        return RelatedContentsResponse(
            scenarios = relatedContents.scenarios.map { mapToItem(it) },
            resources = relatedContents.resources.map { mapToItem(it) },
        )
    }
}

