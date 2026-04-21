package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.model.request.AdminContentOptionBatchUpdateRequest
import com.tropig.backend.contents.model.request.AdminContentOptionRequest
import com.tropig.backend.contents.model.request.AdminContentOptionUpdateRequest
import com.tropig.backend.contents.model.response.AdminContentOptionResponse
import com.tropig.backend.contents.model.response.toAdminResponse
import com.tropig.backend.contents.service.ContentOptionService
import com.tropig.backend.member.enums.Role
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@ApiController
@RequestMapping("/api/admin/content/option")
class AdminContentOptionController(private val contentOptionService: ContentOptionService) {

    @GetMapping("/{type}")
    @RequireAuth
    fun getOptions(
        @LoginMember authMember: AuthMember,
        @PathVariable type: OptionType,
    ): List<AdminContentOptionResponse> {
        checkAdminRole(authMember)
        return contentOptionService.getOptions(type).map { it.toAdminResponse() }
    }

    @PostMapping("/{type}")
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun createOption(
        @LoginMember authMember: AuthMember,
        @PathVariable type: OptionType,
        @RequestBody request: AdminContentOptionRequest,
    ): AdminContentOptionResponse {
        checkAdminRole(authMember)
        return contentOptionService.createOption(type, request).toAdminResponse()
    }

    @PutMapping("/bulk/update")
    @RequireAuth
    fun batchUpdateOptions(
        @LoginMember authMember: AuthMember,
        @RequestBody request: AdminContentOptionBatchUpdateRequest,
    ): List<AdminContentOptionResponse> {
        checkAdminRole(authMember)
        return contentOptionService.batchUpdateOptions(request).map { it.toAdminResponse() }
    }

    @PutMapping("/{id}")
    @RequireAuth
    fun updateOption(
        @LoginMember authMember: AuthMember,
        @PathVariable id: Long,
        @RequestBody request: AdminContentOptionUpdateRequest,
    ): AdminContentOptionResponse {
        checkAdminRole(authMember)
        return contentOptionService.updateOption(id, request).toAdminResponse()
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteOption(@LoginMember authMember: AuthMember, @PathVariable id: Long) {
        checkAdminRole(authMember)
        contentOptionService.deleteOption(id)
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
