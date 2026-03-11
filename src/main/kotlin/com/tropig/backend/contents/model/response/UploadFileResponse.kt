package com.tropig.backend.contents.model.response

import com.tropig.backend.common.enums.FileType
import com.tropig.backend.contents.enums.PublishingType

data class UploadFileResponse(
    val orderNo: Int,
    val uploadedFileName: String,
    val fileType: FileType,
    val publishingType: PublishingType? = null,
    val originalName: String?,
)
