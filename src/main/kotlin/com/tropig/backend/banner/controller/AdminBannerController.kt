package com.tropig.backend.banner.controller

import com.tropig.backend.banner.model.request.BannerImageDeleteRequest
import com.tropig.backend.banner.model.request.BannerImageUploadPresignerRequest
import com.tropig.backend.banner.model.request.CreateBannerRequest
import com.tropig.backend.banner.model.request.UpdateBannerRequest
import com.tropig.backend.banner.model.response.AdminBannerResponse
import com.tropig.backend.banner.model.response.BannerImageUploadPresignerResponse
import com.tropig.backend.banner.service.BannerService
import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.repository.MemberRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@ApiController
@RequestMapping("/api/admin/banners")
class AdminBannerController(private val bannerService: BannerService, private val memberRepository: MemberRepository) {
    @GetMapping
    @RequireAuth
    fun getBanners(@LoginMember authMember: AuthMember): List<AdminBannerResponse> {
        checkAdminRole(authMember)
        return bannerService.getAdminBanners()
    }

    @GetMapping("/{id}")
    @RequireAuth
    fun getBanner(@LoginMember authMember: AuthMember, @PathVariable id: Long): AdminBannerResponse {
        checkAdminRole(authMember)
        return bannerService.getAdminBanner(id)
    }

    @PostMapping
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun createBanner(
        @LoginMember authMember: AuthMember,
        @RequestBody request: CreateBannerRequest,
    ): AdminBannerResponse {
        checkAdminRole(authMember)
        return bannerService.createBanner(request, authMember.memberId)
    }

    @PatchMapping("/{id}")
    @RequireAuth
    fun updateBanner(
        @LoginMember authMember: AuthMember,
        @PathVariable id: Long,
        @RequestBody request: UpdateBannerRequest,
    ): AdminBannerResponse {
        checkAdminRole(authMember)
        return bannerService.updateBanner(id, request, authMember.memberId)
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteBanner(@LoginMember authMember: AuthMember, @PathVariable id: Long) {
        checkAdminRole(authMember)
        bannerService.deleteBanner(id, authMember.memberId)
    }

    @PostMapping("/images/presigner")
    @RequireAuth
    fun createImageUploadPresigner(
        @LoginMember authMember: AuthMember,
        @RequestBody request: BannerImageUploadPresignerRequest,
    ): BannerImageUploadPresignerResponse {
        checkAdminRole(authMember)
        return bannerService.createImageUploadPresigner(request)
    }

    @PostMapping("/html/presigner")
    @RequireAuth
    fun createHtmlUploadPresigner(
        @LoginMember authMember: AuthMember,
        @RequestBody request: BannerImageUploadPresignerRequest,
    ): BannerImageUploadPresignerResponse {
        checkAdminRole(authMember)
        return bannerService.createHtmlUploadPresigner(request)
    }

    @DeleteMapping("/images")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteImage(@LoginMember authMember: AuthMember, @RequestBody request: BannerImageDeleteRequest) {
        checkAdminRole(authMember)
        bannerService.deleteImage(request.path)
    }

    private fun checkAdminRole(authMember: AuthMember) {
        val currentRole = memberRepository.findMemberByIdAndDeletedAtIsNull(authMember.memberId)?.role

        if (currentRole != Role.ADMIN) {
            throw ContentException(
                message = "관리자 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }
    }
}
