package com.tropig.backend.recruitment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.recruitment.enums.PlayEnvironment
import com.tropig.backend.recruitment.model.dto.SearchRecruitmentDto
import com.tropig.backend.recruitment.model.request.CompleteRecruitmentRequest
import com.tropig.backend.recruitment.model.request.CreateApplicationRequest
import com.tropig.backend.recruitment.model.request.CreateRecruitmentRequest
import com.tropig.backend.recruitment.model.response.ApplicationResponse
import com.tropig.backend.recruitment.model.response.RecruitmentDetailResponse
import com.tropig.backend.recruitment.model.response.RecruitmentSummaryResponse
import com.tropig.backend.recruitment.service.RecruitmentApplicationService
import com.tropig.backend.recruitment.service.RecruitmentService
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus

@ApiController
@RequestMapping("/api/recruitments")
class RecruitmentController(
    private val recruitmentService: RecruitmentService,
    private val applicationService: RecruitmentApplicationService,
) {
    @GetMapping
    fun getRecruitments(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) ruleIds: List<Long>?,
        @RequestParam(required = false) environments: List<PlayEnvironment>?,
        @RequestParam(defaultValue = "deadline") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "15") size: Int,
    ): Page<RecruitmentSummaryResponse> = recruitmentService.searchRecruitments(
        SearchRecruitmentDto(
            keyword = keyword,
            ruleIds = ruleIds,
            environments = environments,
            page = page.coerceAtLeast(0),
            size = size.coerceIn(1, 50),
        ),
    )

    @GetMapping("/{id}")
    fun getRecruitment(@LoginMember authMember: AuthMember?, @PathVariable id: Long): RecruitmentDetailResponse =
        recruitmentService.getRecruitment(id, authMember?.memberId)

    @PostMapping
    @RequireAuth
    fun createRecruitment(
        @LoginMember authMember: AuthMember,
        @RequestBody request: CreateRecruitmentRequest,
    ): ResponseEntity<RecruitmentDetailResponse> {
        val response = recruitmentService.createRecruitment(authMember.memberId, request)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    @RequireAuth
    fun updateRecruitment(
        @LoginMember authMember: AuthMember,
        @PathVariable id: Long,
        @RequestBody request: CreateRecruitmentRequest,
    ): RecruitmentDetailResponse = recruitmentService.updateRecruitment(id, authMember.memberId, request)

    @DeleteMapping("/{id}")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecruitment(@LoginMember authMember: AuthMember, @PathVariable id: Long) {
        recruitmentService.deleteRecruitment(id, authMember.memberId)
    }

    @PostMapping("/{id}/complete")
    @RequireAuth
    fun completeRecruitment(
        @LoginMember authMember: AuthMember,
        @PathVariable id: Long,
        @RequestBody request: CompleteRecruitmentRequest,
    ): RecruitmentDetailResponse = recruitmentService.completeRecruitment(id, authMember.memberId, request)

    @PostMapping("/{id}/applications")
    @RequireAuth
    fun applyRecruitment(
        @LoginMember authMember: AuthMember,
        @PathVariable id: Long,
        @RequestBody request: CreateApplicationRequest,
    ): ResponseEntity<ApplicationResponse> {
        val response = applicationService.apply(id, authMember.memberId, request)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}/applications")
    @RequireAuth
    fun getApplications(@LoginMember authMember: AuthMember, @PathVariable id: Long): List<ApplicationResponse> =
        applicationService.getApplications(id, authMember.memberId)
}
