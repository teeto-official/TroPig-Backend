package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.contents.service.ContentService
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/api/content")
class ContentController(
    private val contentService: ContentService,
) {
}