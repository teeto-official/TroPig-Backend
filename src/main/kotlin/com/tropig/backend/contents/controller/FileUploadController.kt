package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.contents.model.request.UploadFileRequest
import com.tropig.backend.contents.model.response.UploadFileResponse
import com.tropig.backend.contents.service.S3Service
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@ApiController
@RequestMapping("/api/files")
class FileUploadController(
    private val s3Service: S3Service
) {
    @PostMapping("/uploads/{contentId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFileWithRollback(
        @RequestParam("request") request: List<UploadFileRequest>,
        @PathVariable contentId: Long
    ): List<UploadFileResponse> {
        if (request.isEmpty()) throw IllegalArgumentException("업로드할 파일이 없습니다.")

        val url = s3Service.uploadFileWithRollback(request, contentId) { uploadedUrl ->
            // 업로드 성공 후 실행할 비즈니스 로직 (예: DB 저장 등)
            //
            // 동작 방식:
            // 1. 파일이 S3에 성공적으로 업로드됨
            // 2. 이 콜백(onSuccess)이 실행됨
            // 3. 여기서 에러가 발생하면 (예: DB 저장 실패)
            // 4. 자동으로 S3에서 업로드된 파일이 삭제됨 (롤백)
            //
            // 예시:
            // contentRepository.save(Content(fileUrl = uploadedUrl, ...))
            // 만약 save()에서 예외가 발생하면 S3의 파일이 자동 삭제됩니다.
            //
            // TODO: 실제 비즈니스 로직 구현
        }

        return url.map { UploadFileResponse(it.first, it.second) }
    }

    @DeleteMapping("/delete")
    fun deleteFile(
        @RequestParam("url") url: String
    ) {
        val s3Key = s3Service.extractS3Key(url)
        s3Service.deleteFile(s3Key)
    }
}
