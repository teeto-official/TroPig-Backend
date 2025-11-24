package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.contents.entity.PickContent
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.model.request.AdminPickContentRequest
import com.tropig.backend.contents.model.response.AdminPickContentResponse
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.PickContentService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

@ApiController
@RequestMapping("/admin/content")
class AdminContentPickController(
    private val contentService: ContentService,
    private val pickContentService: PickContentService,
) {

    @PostMapping("/pick")
    fun setUpPickContent(
        @RequestParam(name = "type", required = true)
        type: ContentType,
        @RequestBody request: List<AdminPickContentRequest>,
    ): List<AdminPickContentResponse> {
        if (request.isEmpty()) return emptyList()

        val originContents = contentService.findByIdInAndType(request.map { it.contentId }, type)
        val orderMap = request.associate { it.contentId to it.orderNo }

        val sortedContents = originContents.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
            .mapIndexed { index, sorted ->
                PickContent(
                    contentId = sorted.id,
                    orderNo = index + 1,
                )
            }

        pickContentService.deleteAll()
        return pickContentService.saveAllPickContents(sortedContents).map {
            AdminPickContentResponse(
                contentId = it.contentId,
                orderNo = it.orderNo
            )
        }
    }
}