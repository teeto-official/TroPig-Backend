package com.tropig.backend.banner.model.request

import com.tropig.backend.banner.enums.BannerType
import java.time.LocalDateTime

data class CreateBannerRequest(
    val title: String,
    val subtitle: String? = null,
    val type: BannerType,
    val pcImagePath: String,
    val mobileImagePath: String? = null,
    val htmlPath: String? = null,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val orderNo: Int = 0,
    val show: Boolean = true,
)
