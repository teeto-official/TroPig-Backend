package com.tropig.backend.banner.model.response

import com.tropig.backend.banner.entity.Banner
import com.tropig.backend.banner.enums.BannerType
import com.tropig.backend.contents.service.S3Service
import java.time.LocalDateTime

data class AdminBannerResponse(
    val id: Long,
    val alias: String,
    val title: String,
    val subtitle: String?,
    val type: BannerType,
    val pcImagePath: String,
    val mobileImagePath: String?,
    val htmlPath: String?,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val orderNo: Int,
    val show: Boolean,
    val lastModifiedAdminId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun Banner.toAdminResponse(s3Service: S3Service): AdminBannerResponse = AdminBannerResponse(
    id = id,
    alias = alias,
    title = title,
    subtitle = subtitle,
    type = type,
    pcImagePath = s3Service.toUrl(pcImagePath) ?: pcImagePath,
    mobileImagePath = s3Service.toUrl(mobileImagePath),
    htmlPath = s3Service.toUrl(htmlPath),
    startedAt = startedAt,
    endedAt = endedAt,
    orderNo = orderNo,
    show = show,
    lastModifiedAdminId = lastModifiedAdminId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
