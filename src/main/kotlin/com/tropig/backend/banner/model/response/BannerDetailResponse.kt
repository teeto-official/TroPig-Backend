package com.tropig.backend.banner.model.response

import com.tropig.backend.banner.entity.Banner
import com.tropig.backend.banner.enums.BannerType
import com.tropig.backend.contents.service.S3Service
import java.time.LocalDateTime

data class BannerDetailResponse(
    val title: String,
    val subtitle: String?,
    val type: BannerType,
    val htmlPath: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
)

fun Banner.toDetailResponse(s3Service: S3Service): BannerDetailResponse? {
    val htmlPath = htmlPath?.let { s3Service.toUrl(it) } ?: return null
    return BannerDetailResponse(
        title = title,
        subtitle = subtitle,
        type = type,
        htmlPath = htmlPath,
        startedAt = startedAt,
        endedAt = endedAt,
    )
}
