package com.tropig.backend.banner.model.response

data class BannerImageUploadPresignerResponse(
    val presignedUrl: String,
    val s3Key: String,
    val publicUrl: String,
    val expiresInSeconds: Int,
    val uuid: String?,
)
