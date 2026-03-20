package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.FileType
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.entity.ContentThumbnail
import com.tropig.backend.contents.enums.PublishingType
import com.tropig.backend.contents.model.request.DeleteFileRequest
import com.tropig.backend.contents.model.request.UploadFilesForm
import com.tropig.backend.contents.model.request.UploadPresignerUrlRequest
import com.tropig.backend.contents.model.response.UploadFileResponse
import com.tropig.backend.contents.model.response.UploadPresignerUrlResponse
import com.tropig.backend.contents.model.serialize.PublishingInfo
import com.tropig.backend.contents.model.serialize.toJson
import com.tropig.backend.contents.model.serialize.toPublishingInfoList
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.member.enums.Role
import jakarta.transaction.Transactional
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import kotlin.math.log

@ApiController
@RequestMapping("/api/files")
class FileUploadController(
    private val s3Service: S3Service,
    private val contentService: ContentService,
) {

    @RequireAuth
    @PostMapping("/uploads/{contentId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Transactional
    fun uploadFileWithRollback(
        @LoginMember authMember: AuthMember,
        @ModelAttribute form: UploadFilesForm,
        @PathVariable contentId: Long,
    ): List<UploadFileResponse> {
        // @ModelAttribute로 ArrayList를 직접 받아 Spring Data projection 문제 방지
        // request[0].type, request[0].file 형식으로 보내야 함

        if (authMember.role != Role.CREATOR) {
            throw MemberException(
                "작가가 아닌 유저입니다.",
                MessageCode.INCORRECT_ROLE,
            )
        }

        contentService.findById(contentId)?.let {
            if (it.memberId != authMember.memberId) {
                throw MemberException(
                    "유저가 작성한 작품이 아닙니다. memberId: ${authMember.memberId}, contentId: ${it.id}",
                    MessageCode.NOT_OWN_CONTENT,
                )
            }
        }

        val fileRequests = form.request.toList()

        if (fileRequests.isEmpty()) {
            throw IllegalArgumentException("업로드할 파일이 없습니다.")
        }

        val url = s3Service.uploadFileWithRollback(fileRequests, contentId) { uploaded ->
            // 업로드 성공 후 실행할 비즈니스 로직 (예: DB 저장 등)
            //
            // 동작 방식:
            // 1. 파일이 S3에 성공적으로 업로드됨
            // 2. 이 콜백(onSuccess)이 실행됨
            // 3. 여기서 에러가 발생하면 (예: DB 저장 실패)
            // 4. 자동으로 S3에서 업로드된 파일이 삭제됨 (롤백)
            //
            uploaded.groupBy { it.fileType }.forEach {
                when (it.key) {
                    FileType.CONTENT_FILE -> {
                        val files = it.value.map { file ->
                            ContentThumbnail(
                                contentId = contentId,
                                path = file.path,
                                orderNo = file.orderNo,
                                cover = file.isCover,
                            )
                        }
                        contentService.saveAllContentThumbnail(files)
                    }
                    FileType.PUBLISHING -> {
                        val files = it.value.map { file ->
                            PublishingInfo(
                                file.publishingType ?: PublishingType.ETC,
                                file.path,
                                file.originalName,
                            )
                        }
                        contentService.findById(contentId)?.let { content ->
                            content.publishingInfo = content.publishingInfo
                                ?.toPublishingInfoList()
                                ?.plus(files)
                                ?.toJson() ?: files.toJson()

                            contentService.save(content)
                        }
                    }
                }
            }
        }

        return url.map {
            UploadFileResponse(
                it.orderNo,
                s3Service.toUrl(it.path) ?: it.path,
                it.fileType,
                it.publishingType,
                it.originalName,
            )
        }
    }

    @RequireAuth
    @PostMapping("/uploads/presigner/{contentId}")
    @Transactional
    fun uploadPresigner(
        @LoginMember authMember: AuthMember,
        @RequestBody form: UploadPresignerUrlRequest,
        @PathVariable contentId: Long,
    ): List<UploadPresignerUrlResponse> {
        if (authMember.role != Role.CREATOR) {
            throw MemberException(
                "작가가 아닌 유저입니다.",
                MessageCode.INCORRECT_ROLE,
            )
        }

        contentService.findById(contentId)?.let {
            if (it.memberId != authMember.memberId) {
                throw MemberException(
                    "유저가 작성한 작품이 아닙니다. memberId: ${authMember.memberId}, contentId: ${it.id}",
                    MessageCode.NOT_OWN_CONTENT,
                )
            }
        }

        if (form.requests.isEmpty()) {
            throw IllegalArgumentException("업로드할 파일이 없습니다.")
        }

        return form.requests.mapNotNull {
            runCatching {
                s3Service.validate(it)
                val extension = s3Service.extractExtension(it.fileName)

                val s3Key = s3Service.generateS3Key(
                    directory = it.fileType.path,
                    extension = extension,
                    contentId = contentId
                )

                val presignedUrl = s3Service.generateUploadPresignerUrl(
                    s3Key = s3Key,
                    contentType = it.contentType,
                )

                UploadPresignerUrlResponse(
                    publicUrl = s3Service.toUrl(s3Key) ?: s3Key,
                    s3Key = s3Key,
                    presignedUrl = presignedUrl,
                    expiresInSeconds = 300,
                    uuid = it.uuid,
                )
            }.onFailure {
                println("${it.message}")
                return@mapNotNull null
            }.getOrNull()
        }
    }

    @DeleteMapping("/delete/{contentId}")
    @Transactional
    fun deleteFile(@RequestBody request: List<DeleteFileRequest>, @PathVariable contentId: Long) {
        request.forEach {
            when (it.type) {
                FileType.CONTENT_FILE -> {
                    val contentThumbnails = contentService.findThumbnailByContentId(contentId)
                        .filter { thumbnail -> it.urls.contains(thumbnail.path) }

                    contentThumbnails.forEach { thumbnail ->
                        deleteFile(thumbnail.path)
                    }

                    contentService.deleteThumbnails(contentThumbnails.map { thumb -> thumb.id })
                }

                FileType.PUBLISHING -> {
                    contentService.findById(contentId)?.let { con ->
                        con.publishingInfo?.toPublishingInfoList()
                            ?.filter { info -> it.urls.contains(info.path) }
                            ?.map { info ->
                                info.path?.let { path -> deleteFile(path) }
                            }

                        con.publishingInfo = con.publishingInfo?.toPublishingInfoList()
                            ?.filterNot { info -> it.urls.contains(info.path) }
                            ?.toJson()

                        contentService.save(con)
                    }
                }
            }
        }
    }

    private fun deleteFile(url: String) {
        val s3Key = s3Service.extractS3Key(url)
        s3Service.deleteFile(s3Key)
    }
}
