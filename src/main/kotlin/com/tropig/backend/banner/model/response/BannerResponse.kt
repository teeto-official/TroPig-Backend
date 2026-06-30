package com.tropig.backend.banner.model.response

import com.tropig.backend.banner.entity.Banner
import com.tropig.backend.banner.enums.BannerDevice
import com.tropig.backend.banner.enums.BannerType
import com.tropig.backend.contents.service.S3Service
import java.time.LocalDateTime

data class BannerResponse(
    val id: Long,
    val alias: String,
    val title: String,
    val subtitle: String?,
    val type: BannerType,
    val imagePath: String,
    val device: BannerDevice,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val orderNo: Int,
)

fun Banner.toDisplayResponse(device: BannerDevice, s3Service: S3Service): BannerResponse {
    val selectedImagePath = when (device) {
        BannerDevice.MOBILE -> mobileImagePath.takeUnless { it.isNullOrBlank() } ?: pcImagePath
        BannerDevice.PC -> pcImagePath
    }
    return BannerResponse(
        id = id,
        alias = alias,
        title = title,
        subtitle = subtitle,
        type = type,
        imagePath = s3Service.toUrl(selectedImagePath) ?: selectedImagePath,
        device = device,
        startedAt = startedAt,
        endedAt = endedAt,
        orderNo = orderNo,
    )
}
