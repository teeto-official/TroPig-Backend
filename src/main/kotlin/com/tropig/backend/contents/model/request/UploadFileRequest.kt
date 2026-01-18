package com.tropig.backend.contents.model.request

import org.springframework.web.multipart.MultipartFile

data class UploadFileRequest(
    val file: MultipartFile,
    val orderNo: Int,
)

class FileInfoRequest(
    val orderNo: Int,
    val fileBytes: ByteArray,
    val contentType: String,
    val originalFileName: String,
)