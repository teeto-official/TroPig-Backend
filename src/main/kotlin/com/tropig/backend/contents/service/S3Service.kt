package com.tropig.backend.contents.service

import com.tropig.backend.config.S3Properties
import com.tropig.backend.contents.model.request.FileInfoRequest
import com.tropig.backend.contents.model.request.UploadFileRequest
import com.tropig.backend.contents.model.result.FileResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
    private val s3AsyncUploadService: S3AsyncUploadService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(S3Service::class.java)
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

    val baseUrl = "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/"

    fun toUrl(key: String?): String? =
        key?.let {
            if (it.startsWith(baseUrl)) it
            else baseUrl + it.trimStart('/')
        }

    /**
     * S3에서 파일을 삭제합니다.
     * @param s3Key 삭제할 파일의 S3 키
     */
    fun deleteFile(s3Key: String) {
        try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(s3Key)
                .build()

            s3Client.deleteObject(deleteObjectRequest)
            logger.info("파일 삭제 성공: s3Key=$s3Key")
        } catch (e: Exception) {
            logger.error("파일 삭제 실패: s3Key=$s3Key", e)
            throw RuntimeException("파일 삭제에 실패했습니다: ${e.message}", e)
        }
    }

    /**
     * S3 URL에서 키를 추출합니다.
     * @param url S3 URL
     * @return S3 키
     */
    fun extractS3Key(url: String): String = when {
        url.contains("amazonaws.com/") -> {
            url.substringAfter("amazonaws.com/")
        }
        else -> {
            // 이미 키인 경우
            if (!url.contains("http")) {
                url
            } else {
                throw IllegalArgumentException("잘못된 S3 URL 형식입니다: $url")
            }
        }
    }

    /**
     * S3에서 key로 파일을 가져와서 텍스트로 반환합니다.
     * @param key S3 키 (또는 S3 URL)
     * @return 파일 내용을 String으로 반환
     * @throws IllegalArgumentException txt 파일이 아닌 경우
     * @throws RuntimeException 파일을 가져오는데 실패한 경우
     */
    fun getFileAsString(key: String): String {
        try {
            // URL인 경우 키 추출
            val s3Key = if (key.contains("amazonaws.com/")) {
                extractS3Key(key)
            } else {
                key
            }

            // txt 파일인지 확인 (확장자 체크)
            if (!s3Key.lowercase().endsWith(".txt")) {
                throw IllegalArgumentException("txt 파일만 읽을 수 있습니다. 파일: $s3Key")
            }

            val getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(s3Key)
                .build()

            val response: ResponseInputStream<GetObjectResponse> = s3Client.getObject(getObjectRequest)

            // Content-Type도 확인 (추가 검증)
            val contentType = response.response().contentType()
            if (contentType != null && !contentType.startsWith("text/")) {
                logger.warn("Content-Type이 text가 아닙니다: $contentType, key=$s3Key")
            }

            return response.use { inputStream ->
                inputStream.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            logger.error("S3 파일 읽기 실패: key=$key", e)
            throw RuntimeException("S3에서 파일을 가져오는데 실패했습니다: ${e.message}", e)
        }
    }

    /**
     * 일반 파일(이미지 포함)을 S3에 업로드합니다. (동기)
     * @param inputStream 업로드할 파일의 InputStream
     * @param contentType 파일의 MIME 타입 (예: application/pdf, image/jpeg)
     * @param originalFileName 원본 파일명
     * @param contentId 컨텐츠 ID
     * @return 업로드된 파일의 S3 Key
     * @throws IllegalArgumentException 허용되지 않는 파일 타입이거나 크기가 초과된 경우
     */
    fun uploadFile(
        inputStream: InputStream,
        contentType: String,
        originalFileName: String,
        id: Long,
        isMember: Boolean = false,
    ): String {
        val fileBytes = inputStream.readAllBytes()
        return uploadFileBytes(fileBytes, contentType, originalFileName, id, isMember)
    }

    /**
     * 기존 S3 key를 사용하여 파일을 업데이트합니다.
     * @param inputStream 업로드할 파일의 InputStream
     * @param contentType 파일의 MIME 타입
     * @param s3Key 업데이트할 S3 key (기존 key)
     * @return 업로드된 파일의 S3 Key (입력한 key와 동일)
     */
    fun updateFile(inputStream: InputStream, contentType: String, s3Key: String): String {
        val fileBytes = inputStream.readAllBytes()

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

        try {
            // S3에 업로드 (기존 key 사용)
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(s3Key)
                .contentType(contentType)
                .build()

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes))

            logger.info("파일 업데이트 성공: s3Key=$s3Key, contentType=$contentType, size=${fileBytes.size} bytes")

            // S3 Key 반환
            return s3Key
        } catch (e: Exception) {
            logger.error("파일 업데이트 실패: s3Key=$s3Key", e)
            throw RuntimeException("파일 업데이트에 실패했습니다: ${e.message}", e)
        }
    }

    /**
     * 바이트 배열을 사용하여 S3에 파일을 업로드합니다. (내부 구현 - 동기)
     * @param fileBytes 업로드할 파일의 바이트 배열
     * @param contentType 파일의 MIME 타입
     * @param originalFileName 원본 파일명
     * @param contentId 컨텐츠 ID
     * @return 업로드된 파일의 S3 Key
     * @throws IllegalArgumentException 허용되지 않는 파일 타입이거나 크기가 초과된 경우
     */
    private fun uploadFileBytes(
        fileBytes: ByteArray,
        contentType: String,
        originalFileName: String,
        contentId: Long,
        isMember: Boolean,
    ): String {
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
        val s3Key = if (isMember) "member/$contentId/$fileName" else "public/$contentId/$fileName"

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
            return s3Key
        } catch (e: Exception) {
            logger.error("파일 업로드 실패: s3Key=$s3Key", e)
            throw RuntimeException("파일 업로드에 실패했습니다: ${e.message}", e)
        }
    }

    /**
     * 파일을 업로드하고, 에러 발생 시 자동으로 롤백합니다.
     * 파일 업로드는 비동기로 처리됩니다.
     * @param request 업로드할 파일 리스트
     * @param contentId 컨텐츠 ID
     * @param onSuccess 업로드 성공 후 실행할 작업 (업로드된 S3 Key를 파라미터로 받음)
     * @return 업로드된 파일의 목록 (orderNo, s3Key, fileType)
     */
    fun uploadFileWithRollback(
        request: List<UploadFileRequest>,
        contentId: Long,
        onSuccess: (List<FileResult>) -> Unit,
    ): List<FileResult> {
        // InputStream을 미리 바이트 배열로 변환 (비동기 처리 전에 읽어야 함)
        val fileDataList = request.mapNotNull {
            it.file?.let { f ->
                val fileBytes = f.inputStream.readAllBytes()
                val contentType = f.contentType ?: "application/content"
                val originalFileName = f.originalFilename ?: "${contentId}_${it.orderNo}"
                FileInfoRequest(
                    it.orderNo!!,
                    fileBytes,
                    contentType,
                    originalFileName,
                    it.type!!,
                    it.isCover!!,
                    it.publishingType,
                )
            }
        }

        // 모든 파일을 비동기로 업로드 (별도 서비스를 통해 호출하여 프록시가 작동하도록 함)
        val uploadFutures = fileDataList.map {
            s3AsyncUploadService.uploadFileAsync(
                it.fileBytes,
                it.contentType,
                it.originalFileName,
                contentId,
                it.type.path,
            )
                .thenApply { s3Key -> FileResult(it.orderNo, s3Key, it.type, it.isCover, it.publishingType) }
                .exceptionally { throwable ->
                    logger.error("파일 업로드 실패: orderNo=${it.orderNo}", throwable)
                    throw RuntimeException("파일 업로드에 실패했습니다: ${throwable.message}", throwable)
                }
        }

        // 업로드 성공한 파일들의 S3 Key 저장 (롤백용)
        val uploadedKeys = mutableListOf<String>()

        return try {
            // 모든 업로드가 완료될 때까지 대기
            val uploadResults = CompletableFuture.allOf(*uploadFutures.toTypedArray())
                .thenApply {
                    uploadFutures.map { it.get() }
                }
                .get() // 동기적으로 대기 (롤백 처리를 위해)

            // 각 파일에 대해 onSuccess 콜백 실행
            uploadResults.forEach { (_, s3Key) ->
                uploadedKeys.add(s3Key)
            }
            onSuccess(uploadResults)
            uploadResults
        } catch (e: Exception) {
            // 에러 발생 시 업로드된 모든 파일 삭제 (롤백)
            // uploadedKeys는 onSuccess 호출 전에 채워지지 않을 수 있으므로,
            // 실제로 업로드된 파일들을 찾아서 삭제해야 함
            uploadFutures.forEachIndexed { index, future ->
                try {
                    if (future.isDone && !future.isCompletedExceptionally) {
                        val (_, s3Key) = future.get()
                        if (!uploadedKeys.contains(s3Key)) {
                            uploadedKeys.add(s3Key)
                        }
                    }
                } catch (ex: Exception) {
                    // Future가 실패했거나 아직 완료되지 않은 경우 무시
                    logger.debug("파일 업로드 미완료 또는 실패: index=$index")
                }
            }

            // 업로드된 모든 파일 삭제
            uploadedKeys.forEach { s3Key ->
                try {
                    deleteFile(s3Key)
                    logger.info("에러 발생으로 인한 파일 롤백 완료: s3Key=$s3Key")
                } catch (deleteException: Exception) {
                    logger.error("롤백 중 파일 삭제 실패: s3Key=$s3Key", deleteException)
                }
            }
            throw e
        }
    }
}
