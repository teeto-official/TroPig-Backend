package com.tropig.backend.banner.model.request

import com.tropig.backend.banner.enums.BannerType
import java.time.LocalDateTime

data class UpdateBannerRequest(
    val title: String? = null,
    val subtitle: String? = null,
    val type: BannerType? = null,
    val pcImagePath: String? = null,
    val mobileImagePath: String? = null,
    val htmlPath: String? = null,
    val startedAt: LocalDateTime? = null,
    val endedAt: LocalDateTime? = null,
    val orderNo: Int? = null,
    val show: Boolean? = null,
)
