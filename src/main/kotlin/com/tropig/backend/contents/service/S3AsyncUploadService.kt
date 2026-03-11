package com.tropig.backend.contents.service

import com.tropig.backend.config.S3Properties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class S3AsyncUploadService(private val s3Client: S3Client, private val s3Properties: S3Properties) {
    companion object {
        private val logger = LoggerFactory.getLogger(S3AsyncUploadService::class.java)
        private val ALLOWED_FILE_TYPES = setOf(
            // 이미지
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            // 문서
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            // 압축 파일
            "application/zip",
            "application/x-zip-compressed",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            // 텍스트
            "text/plain",
            "text/csv",
            // 기타
            "application/json",
            "application/xml",
        )
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
    }

    /**
     * 바이트 배열을 사용하여 S3에 파일을 업로드합니다. (비동기)
     * @param fileBytes 업로드할 파일의 바이트 배열
     * @param contentType 파일의 MIME 타입
     * @param originalFileName 원본 파일명
     * @param contentId 컨텐츠 ID
     * @return 업로드된 파일의 S3 Key를 담은 CompletableFuture
     * @throws IllegalArgumentException 허용되지 않는 파일 타입이거나 크기가 초과된 경우
     */
    @Async("s3UploadExecutor")
    fun uploadFileAsync(
        fileBytes: ByteArray,
        contentType: String,
        originalFileName: String,
        contentId: Long,
        prefix: String,
    ): CompletableFuture<String> {
        // 파일 타입 검증
        if (!ALLOWED_FILE_TYPES.contains(contentType.lowercase())) {
            throw IllegalArgumentException(
                "허용되지 않는 파일 타입입니다. 지원 형식: 이미지(JPEG, PNG, GIF, WEBP), " +
                    "문서(PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX), " +
                    "압축파일(ZIP, RAR, 7Z), " +
                    "기타(JSON, XML, TXT, CSV)",
            )
        }

        // 파일 크기 검증
        if (fileBytes.size > MAX_FILE_SIZE) {
            throw IllegalArgumentException("파일 크기가 너무 큽니다. 최대 크기: ${MAX_FILE_SIZE / 1024 / 1024}MB")
        }

        // 파일명 생성 (UUID + 원본 파일명)
        val fileName = "${UUID.randomUUID()}-$originalFileName"
        val s3Key = "$prefix/$contentId/$fileName"

        try {
            // S3에 업로드
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(s3Key)
                .contentType(contentType)
                .build()

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes))

            logger.info("파일 업로드 성공: s3Key=$s3Key, contentType=$contentType, size=${fileBytes.size} bytes")

            // S3 Key 반환
            return CompletableFuture.completedFuture(s3Key)
        } catch (e: Exception) {
            logger.error("파일 업로드 실패: s3Key=$s3Key", e)
            throw RuntimeException("파일 업로드에 실패했습니다: ${e.message}", e)
        }
    }
}
