package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.contents.entity.PickContent
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.request.AdminPickContentRequest
import com.tropig.backend.contents.model.response.AdminPickContentResponse
import com.tropig.backend.contents.service.ContentMigrationService
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.MigrateContentTagResult
import com.tropig.backend.contents.service.PickContentService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@ApiController
@RequestMapping("/admin/content")
class AdminContentPickController(
    private val contentService: ContentService,
    private val pickContentService: PickContentService,
    private val contentMigrationService: ContentMigrationService,
) {

    companion object {
        const val MAX_VALUE = Int.MAX_VALUE
    }

    @PostMapping("/pick")
    fun setUpPickContent(
        @RequestParam(name = "type", required = true)
        type: ContentType,
        @RequestBody request: List<AdminPickContentRequest>,
    ): List<AdminPickContentResponse> {
        if (request.isEmpty()) return emptyList()

        val originContents = contentService.findByIdInAndType(request.map { it.contentId }, type)
        val orderMap = request.associate { it.contentId to it.orderNo }

        val sortedContents = originContents.sortedBy { orderMap[it.id] ?: MAX_VALUE }
            .mapIndexed { index, sorted ->
                PickContent(
                    contentId = sorted.id,
                    orderNo = index + 1,
                )
            }

        return pickContentService.updatePickContents(sortedContents).map {
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
    fun migrateRuleAndGenre(): MigrateContentTagResult =
        contentMigrationService.migrateRuleAndGenreToIds()
}
