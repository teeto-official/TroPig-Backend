package com.tropig.backend.contents.model.response

data class UploadPresignerUrlResponse(
    val presignedUrl: String,
    val s3Key: String,
    val publicUrl: String,
    val expiresInSeconds: Int,
    val uuid: String,
)
