package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.FileType
import com.tropig.backend.contents.enums.PublishingType
import org.springframework.web.multipart.MultipartFile

data class UploadFileRequest(
    var type: FileType? = null,
    var file: MultipartFile? = null,
    var orderNo: Int? = null,
    var isCover: Boolean? = null,
    var publishingType: PublishingType? = null,
)

data class UploadFilesForm(
    var request: MutableList<UploadFileRequest> = mutableListOf()
)


data class DeleteFileRequest(
    val type: FileType,
    val urls: List<String>,
)

class FileInfoRequest(
    val orderNo: Int,
    val fileBytes: ByteArray,
    val contentType: String,
    val originalFileName: String,
    val type: FileType,
    val isCover: Boolean,
    val publishingType: PublishingType?
)