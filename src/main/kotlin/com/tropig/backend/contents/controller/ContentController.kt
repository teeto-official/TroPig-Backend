package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.response.PickContentResponse
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.PickContentService
import com.tropig.backend.member.service.MemberService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/api/content")
class ContentController(
    private val contentService: ContentService,
    private val pickContentService: PickContentService,
    private val memberService: MemberService
) {

    @GetMapping("/pick/{type}")
    fun getPickContent(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember?,
        @PathVariable
        type: ContentType
    ): List<PickContentResponse> {
        val isAdult = authMember?.adult ?: false
        val pickContents = pickContentService.getAllPickContent()
        val contents = contentService.getPickContentsByType(type, isAdult, pickContents.map { it.contentId })
        val writerName = memberService.getWritersName(contents.map { it.writerId })
        val contentsMap = contents.associateBy { it.id }

        return pickContents.mapNotNull {
            val content = contentsMap[it.contentId] ?: return@mapNotNull null

            PickContentResponse(
                title = content.title,
                alias = content.alias,
                thumbnailPath = content.thumbnailPath,
                writer = writerName[content.writerId] ?: "",
                tags = content.tags,
                orderNo = it.orderNo
            )
        }.sortedBy { it.orderNo }
    }
}