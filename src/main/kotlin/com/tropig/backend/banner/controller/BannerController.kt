package com.tropig.backend.banner.controller

import com.tropig.backend.banner.enums.BannerDevice
import com.tropig.backend.banner.model.response.BannerDetailResponse
import com.tropig.backend.banner.model.response.BannerResponse
import com.tropig.backend.banner.service.BannerService
import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@ApiController
@RequestMapping("/api/banners")
class BannerController(private val bannerService: BannerService) {
    @GetMapping
    fun getBanners(@RequestParam(defaultValue = "PC") device: BannerDevice): List<BannerResponse> =
        bannerService.getDisplayBanners(device)

    @GetMapping("/{alias}")
    fun getBanner(
        @PathVariable alias: String,
    ): BannerDetailResponse =
        bannerService.getDisplayBanner(alias) ?: throw NotFoundException(
            code = MessageCode.NOT_FOUND_BANNER,
            message = "해당 배너는 유효하지 않습니다."
        )
}
