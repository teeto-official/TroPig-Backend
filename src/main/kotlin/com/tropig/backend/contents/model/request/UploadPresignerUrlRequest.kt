package com.tropig.backend.contents.model.request

import com.tropig.backend.common.enums.FileType

data class UploadPresignerUrlRequest(val requests: List<UploadPresignerUrlForm>)

data class UploadPresignerUrlForm(
    val fileName: String,
    val contentType: String,
    val fileType: FileType,
    val uuid: String,
)
