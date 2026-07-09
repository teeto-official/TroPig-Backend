package com.tropig.backend.recruitment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.recruitment.enums.RecruitActivityType
import com.tropig.backend.recruitment.model.response.RecruitAlertResponse
import com.tropig.backend.recruitment.service.RecruitAlertService
import com.tropig.backend.recruitment.service.RecruitmentApplicationService
import com.tropig.backend.recruitment.service.RecruitmentService
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import com.tropig.backend.common.exception.IllegalArgumentException as TroPigIllegalArgumentException

@ApiController
@RequestMapping("/api/my")
@RequireAuth
class MyRecruitmentController(
    private val recruitmentService: RecruitmentService,
    private val applicationService: RecruitmentApplicationService,
    private val recruitAlertService: RecruitAlertService,
) {
    @GetMapping("/recruitments")
    fun getMyRecruitments(
        @LoginMember authMember: AuthMember,
        @RequestParam type: String,
        @RequestParam(defaultValue = "0") page: Int,
    ): Page<*> = when (parseActivityType(type)) {
        RecruitActivityType.HOSTING ->
            recruitmentService.getMyHostingRecruitments(authMember.memberId, page.coerceAtLeast(0))
        RecruitActivityType.APPLIED ->
            applicationService.getMyApplications(authMember.memberId, page.coerceAtLeast(0))
    }

    @GetMapping("/recruit-alert")
    fun getRecruitAlert(@LoginMember authMember: AuthMember): RecruitAlertResponse =
        recruitAlertService.getAlert(authMember.memberId)

    @PostMapping("/recruit-alert/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readRecruitAlert(@LoginMember authMember: AuthMember, @RequestParam type: String) {
        recruitAlertService.markRead(authMember.memberId, parseActivityType(type))
    }

    private fun parseActivityType(type: String): RecruitActivityType = runCatching {
        RecruitActivityType.valueOf(type.trim().uppercase())
    }.getOrElse {
        throw TroPigIllegalArgumentException("유효하지 않은 type 입니다. type: $type", MessageCode.INVALID_PARAMS)
    }
}
