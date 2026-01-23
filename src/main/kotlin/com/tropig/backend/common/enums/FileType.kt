package com.tropig.backend.common.enums

enum class FileType(private val tableName: String, val path: String) {
    CONTENT_FILE("content_file", "public"),
    PUBLISHING("content", "downloads"),
}