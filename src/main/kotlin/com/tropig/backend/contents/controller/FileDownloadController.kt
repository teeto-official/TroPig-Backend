package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.model.response.DownloadResponse
import com.tropig.backend.contents.model.serialize.toPublishingInfoList
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.payment.service.PaymentContentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@ApiController
@RequestMapping("/api/files")
class FileDownloadController(
    private val s3Service: S3Service,
    private val contentService: ContentService,
    private val paymentContentService: PaymentContentService,
) {
    /**
     * 구매한 컨텐츠의 다운로드 파일에 대한 Presigned URL을 발급합니다.
     * @param contentId 컨텐츠 ID
     * @param s3Key 다운로드할 파일의 S3 키
     * @return Presigned URL
     */
    @RequireAuth
    @GetMapping("/download/{contentId}")
    fun getDownloadUrl(
        @LoginMember authMember: AuthMember,
        @PathVariable contentId: Long,
        @RequestParam s3Key: String,
    ): DownloadResponse {
        val content = contentService.findById(contentId)
            ?: throw ContentException("컨텐츠를 찾을 수 없습니다.", MessageCode.NOT_FOUND_CONTENT)

        // 무료 컨텐츠(price == 0)가 아닌 경우 구매 여부 확인
        if (content.price > 0) {
            // 작성자 본인은 다운로드 가능
            val isOwner = content.memberId == authMember.memberId
            val isPurchased = paymentContentService.isContentPurchased(authMember.memberId, contentId)

            if (!isOwner && !isPurchased) {
                throw ContentException("구매 후 다운로드할 수 있습니다.", MessageCode.PURCHASE_REQUIRED)
            }
        }

        // 요청된 s3Key가 해당 컨텐츠의 다운로드 파일인지 검증
        val publishingInfoList = content.publishingInfo?.toPublishingInfoList() ?: emptyList()
        val matchedFile = publishingInfoList.find { it.path == s3Key }
            ?: throw ContentException("해당 컨텐츠의 다운로드 파일이 아닙니다.", MessageCode.NOT_FOUND_CONTENT_INFO)

        val fileName = matchedFile.originalName?.takeIf { it.isNotBlank() }
        val presignedUrl = s3Service.generatePresignedUrl(s3Key, fileName = fileName)

        return DownloadResponse(presignedUrl)
    }
}
