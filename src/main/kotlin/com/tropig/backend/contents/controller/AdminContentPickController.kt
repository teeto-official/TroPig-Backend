package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.entity.PickContent
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.request.AdminPickContentRequest
import com.tropig.backend.contents.model.response.AdminPickContentDetailResponse
import com.tropig.backend.contents.model.response.AdminPickContentResponse
import com.tropig.backend.contents.service.ContentMigrationService
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.MigrateContentTagResult
import com.tropig.backend.contents.service.PickContentService
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.service.CreatorService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@ApiController
@RequestMapping("/api/admin/content")
class AdminContentPickController(
    private val contentService: ContentService,
    private val pickContentService: PickContentService,
    private val contentMigrationService: ContentMigrationService,
    private val creatorService: CreatorService,
    private val s3Service: S3Service,
) {

    companion object {
        const val MAX_VALUE = Int.MAX_VALUE
    }

    @GetMapping("/pick/{type}")
    @RequireAuth
    fun getPickContent(
        @LoginMember authMember: AuthMember,
        @PathVariable type: ContentType,
    ): List<AdminPickContentDetailResponse> {
        checkAdminRole(authMember)

        val allPickContents = pickContentService.getAllPickContent()
        val allContentIds = allPickContents.map { it.contentId }
        val contents = contentService.findByIdInAndType(allContentIds, type)
        val writerNames = creatorService.getWritersName(contents.map { it.memberId })
        val thumbnails = contentService.getThumbnailPath(allContentIds).associateBy { it.contentId }
        val contentsMap = contents.associateBy { it.id }

        return allPickContents.mapNotNull { pick ->
            val content = contentsMap[pick.contentId] ?: return@mapNotNull null
            AdminPickContentDetailResponse(
                id = content.id,
                title = content.title,
                alias = content.alias,
                thumbnailPath = s3Service.toUrl(thumbnails[content.id]?.path),
                writer = writerNames[content.memberId] ?: "",
                type = content.type.name,
                orderNo = pick.orderNo,
            )
        }
    }

    @PostMapping("/pick")
    @RequireAuth
    fun setUpPickContent(
        @LoginMember authMember: AuthMember,
        @RequestParam(name = "type", required = true)
        type: ContentType,
        @RequestBody request: List<AdminPickContentRequest>,
    ): List<AdminPickContentResponse> {
        checkAdminRole(authMember)

        val existingPickContentIds = pickContentService.getAllPickContent()
            .map { it.contentId }
            .let { ids -> contentService.findByIdInAndType(ids, type).map { it.id } }

        if (request.isEmpty()) {
            pickContentService.updatePickContentsByType(existingPickContentIds, emptyList())
            return emptyList()
        }

        val originContents = contentService.findByIdInAndType(request.map { it.contentId }, type)
        val orderMap = request.associate { it.contentId to it.orderNo }

        val sortedContents = originContents.sortedBy { orderMap[it.id] ?: MAX_VALUE }
            .mapIndexed { index, sorted ->
                PickContent(
                    contentId = sorted.id,
                    orderNo = index + 1,
                )
            }

        return pickContentService.updatePickContentsByType(existingPickContentIds, sortedContents).map {
            AdminPickContentResponse(
                contentId = it.contentId,
                orderNo = it.orderNo,
            )
        }
    }

    @PostMapping("/migrate/rule-genre")
    @Operation(
        summary = "콘텐츠 rule/genre ID 마이그레이션",
        description = "content 테이블의 구 rule/genre 컬럼(enum 이름)을 rule_id/genre_id(content_option ID)로 일괄 변환합니다. " +
            "완료 후 content_tag_master_cleanup.sql을 실행하여 구 컬럼을 삭제하세요.",
    )
    fun migrateRuleAndGenre(): MigrateContentTagResult = contentMigrationService.migrateRuleAndGenreToIds()

    private fun checkAdminRole(authMember: AuthMember) {
        if (authMember.role != Role.ADMIN) {
            throw ContentException(
                message = "관리자 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }
    }
}
