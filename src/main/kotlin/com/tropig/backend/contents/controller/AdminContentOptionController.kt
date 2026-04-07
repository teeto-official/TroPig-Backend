package com.tropig.backend.contents.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.entity.ContentOption
import com.tropig.backend.contents.model.request.AdminContentOptionRequest
import com.tropig.backend.contents.model.request.AdminContentOptionUpdateRequest
import com.tropig.backend.contents.model.response.AdminContentOptionResponse
import com.tropig.backend.contents.model.response.toAdminResponse
import com.tropig.backend.contents.repository.ContentOptionRepository
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
class AdminContentOptionController(private val contentOptionRepository: ContentOptionRepository) {

    @GetMapping("/{type}")
    @RequireAuth
    fun getOptions(
        @LoginMember authMember: AuthMember,
        @PathVariable type: OptionType,
    ): List<AdminContentOptionResponse> {
        checkAdminRole(authMember)
        return contentOptionRepository.findAllByType(type)
            .sortedBy { it.sortOrder }
            .map { it.toAdminResponse() }
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
        val option = ContentOption(
            type = type,
            name = request.name,
            displayName = request.displayName,
            show = request.show,
            sortOrder = request.sortOrder,
        )
        return contentOptionRepository.save(option).toAdminResponse()
    }

    @PutMapping("/{id}")
    @RequireAuth
    fun updateOption(
        @LoginMember authMember: AuthMember,
        @PathVariable id: Long,
        @RequestBody request: AdminContentOptionUpdateRequest,
    ): AdminContentOptionResponse {
        checkAdminRole(authMember)
        val existing = contentOptionRepository.findById(id).orElseThrow {
            NotFoundException("해당 옵션을 찾을 수 없습니다.", MessageCode.NOT_FOUND_CONTENT_INFO)
        }
        val updated = existing.copy(
            displayName = request.displayName,
            show = request.show,
            sortOrder = request.sortOrder,
        )
        return contentOptionRepository.save(updated).toAdminResponse()
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteOption(@LoginMember authMember: AuthMember, @PathVariable id: Long) {
        checkAdminRole(authMember)
        if (!contentOptionRepository.existsById(id)) {
            throw NotFoundException("해당 옵션을 찾을 수 없습니다.", MessageCode.NOT_FOUND_CONTENT_INFO)
        }
        contentOptionRepository.deleteById(id)
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
