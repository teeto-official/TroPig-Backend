package com.tropig.backend.banner.model.request

data class BannerImageUploadPresignerRequest(val fileName: String, val contentType: String, val uuid: String? = null)
