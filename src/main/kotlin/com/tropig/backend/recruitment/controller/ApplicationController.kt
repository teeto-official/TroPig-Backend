package com.tropig.backend.recruitment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.recruitment.model.response.ApplicationResponse
import com.tropig.backend.recruitment.service.RecruitmentApplicationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@ApiController
@RequestMapping("/api/applications")
class ApplicationController(private val applicationService: RecruitmentApplicationService) {
    @GetMapping("/{id}")
    @RequireAuth
    fun getApplication(@LoginMember authMember: AuthMember, @PathVariable id: Long): ApplicationResponse =
        applicationService.getApplication(id, authMember.memberId)
}
