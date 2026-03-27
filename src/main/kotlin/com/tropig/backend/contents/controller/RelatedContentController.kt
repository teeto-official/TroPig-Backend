package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.model.response.RelatedContentItemResponse
import com.tropig.backend.contents.model.response.RelatedContentsResponse
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.RelatedContentService
import com.tropig.backend.contents.service.S3Service
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
    private val s3Service: S3Service,
) {

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

        fun mapToItem(content: Content): RelatedContentItemResponse = RelatedContentItemResponse(
            id = content.id,
            title = content.title,
            alias = content.alias,
            rule = content.rule,
            playerCountType = content.playerCountType,
            thumbnailPath = s3Service.toUrl(thumbnailPaths[content.id]),
            tags = tagsByContentId[content.id].orEmpty(),
            writer = writerNames[content.memberId] ?: "탈퇴한 작가입니다.",
            price = content.price.toInt(),
        )

        return RelatedContentsResponse(
            scenarios = relatedContents.scenarios.map { mapToItem(it) },
            resources = relatedContents.resources.map { mapToItem(it) },
        )
    }
}
