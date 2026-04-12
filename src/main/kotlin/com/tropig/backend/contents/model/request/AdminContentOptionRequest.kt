package com.tropig.backend.contents.model.request

data class AdminContentOptionRequest(
    val name: String,
    val displayName: String,
    val show: Boolean = true,
    val sortOrder: Int = 0,
)

data class AdminContentOptionUpdateRequest(val displayName: String, val show: Boolean, val sortOrder: Int)
