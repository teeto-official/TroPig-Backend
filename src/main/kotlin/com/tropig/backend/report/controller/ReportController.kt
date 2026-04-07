package com.tropig.backend.report.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.IllegalArgumentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.enums.Role
import com.tropig.backend.report.model.request.CreateReportRequest
import com.tropig.backend.report.model.response.ReportResponse
import com.tropig.backend.report.service.ReportService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/api/report")
class ReportController(private val reportService: ReportService) {

    @PostMapping
    @RequireAuth
    fun createReport(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: CreateReportRequest,
    ): ResponseEntity<ReportResponse> {
        val response = reportService.createReport(authMember.memberId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @RequireAuth
    fun getReports(@LoginMember authMember: AuthMember): List<ReportResponse> {
        if (authMember.role != Role.ADMIN) {
            throw IllegalArgumentException("관리자 권한이 필요합니다.", MessageCode.INCORRECT_ROLE)
        }
        return reportService.getReports()
    }
}
