package com.tropig.backend.contents.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import org.springframework.stereotype.Service

@Service
class AdminBanService(
    private val contentService: ContentService,
) {

    fun banContent(contentId: Long) {
        val content = contentService.findById(contentId)
            ?: throw NotFoundException("콘텐츠를 찾을 수 없습니다.", MessageCode.NOT_FOUND_CONTENT)
        contentService.banContent(content)
    }
}
