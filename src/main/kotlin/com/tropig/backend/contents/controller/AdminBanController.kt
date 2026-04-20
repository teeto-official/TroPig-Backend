package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.service.AdminBanService
import com.tropig.backend.member.enums.Role
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@ApiController
@RequestMapping("/api/admin/ban")
class AdminBanController(
    private val adminBanService: AdminBanService,
) {

    @PatchMapping("/content/{contentId}")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun banContent(@LoginMember authMember: AuthMember, @PathVariable contentId: Long) {
        checkAdminRole(authMember)
        adminBanService.banContent(contentId)
    }

    private fun checkAdminRole(authMember: AuthMember) {
        if (authMember.role != Role.ADMIN) {
            throw ContentException(
                message = "관리자 권한이 필요합니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }
    }
}
