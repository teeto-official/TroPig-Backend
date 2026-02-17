package com.tropig.backend.payment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.payment.model.response.RevenueItemResponse
import com.tropig.backend.payment.model.response.RevenueSummaryResponse
import com.tropig.backend.payment.service.RevenueService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ApiController
@RequestMapping("/api/revenue")
@Tag(name = "Revenue", description = "크리에이터 수익 조회 API")
class RevenueController(
    private val revenueService: RevenueService,
) {

    @RequireAuth
    @GetMapping("/{type}/list")
    @Operation(summary = "수익 리스트 조회", description = "CREATOR가 등록한 작품의 구매 완료 수익 리스트를 조회합니다.")
    fun getRevenueList(
        @LoginMember authMember: AuthMember,
        @PathVariable type: ContentType,
    ): List<RevenueItemResponse> {
        val contents = revenueService.getCreatorContents(authMember.memberId, type)
        if (contents.isEmpty()) {
            return emptyList()
        }

        return revenueService.getRevenueItems(authMember, contents)
    }

    @RequireAuth
    @GetMapping("/summary")
    @Operation(summary = "전체 수익 조회", description = "CREATOR가 등록한 작품의 전체 수익과 출금 완료 금액, 잔여 수익을 조회합니다.")
    fun getRevenueSummary(
        @LoginMember authMember: AuthMember,
    ): RevenueSummaryResponse {
        val contents = revenueService.getAllCreatorContents(authMember.memberId)
        if (contents.isEmpty()) {
            return RevenueSummaryResponse(
                totalRevenue = 0L,
                withdrawnAmount = 0L,
                availableRevenue = 0L,
            )
        }

        return revenueService.getRevenueSummary(authMember, contents)
    }
}

