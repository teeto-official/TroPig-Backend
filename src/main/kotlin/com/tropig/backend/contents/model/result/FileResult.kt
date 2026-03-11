package com.tropig.backend.contents.model.result

import com.tropig.backend.common.enums.FileType
import com.tropig.backend.contents.enums.PublishingType

data class FileResult(
    val orderNo: Int,
    val path: String,
    val fileType: FileType,
    val isCover: Boolean,
    val publishingType: PublishingType?,
    val originalName: String?,
)
