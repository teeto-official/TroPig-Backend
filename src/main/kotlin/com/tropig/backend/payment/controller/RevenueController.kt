package com.tropig.backend.payment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.payment.model.request.RevenueListRequest
import com.tropig.backend.payment.model.response.RevenueItemResponse
import com.tropig.backend.payment.model.response.RevenueSummaryResponse
import com.tropig.backend.payment.service.RevenueService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDateTime

@ApiController
@RequestMapping("/api/revenue")
@Tag(name = "Revenue", description = "크리에이터 수익 조회 API")
class RevenueController(
    private val revenueService: RevenueService,
) {

    @RequireAuth
    @GetMapping("/list")
    @Operation(summary = "수익금 - 수익 목록 조회", description = "창작자가 등록한 작품의 구매 완료 수익 리스트를 조회합니다.")
    fun getRevenueList(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @Parameter(name = "cursorId", description = "커서 revenueId", `in` = ParameterIn.QUERY)
        cursorId: Long? = null,
        @Parameter(name = "cursorCreatedAt", description = "커서 등록일자", `in` = ParameterIn.QUERY)
        cursorCreatedAt: LocalDateTime? = null,
        @Parameter(name = "size", description = "페이지 크기", `in` = ParameterIn.QUERY)
        size: Int = 15,
    ): CursorSlice<RevenueItemResponse> {
        val request =  RevenueListRequest(
            cursorCreatedAt = cursorCreatedAt, cursorId = cursorId ?: 0, size = size
        )
        val contents = revenueService.getAllCreatorContents(authMember.memberId)
        if (contents.isEmpty()) {
            return CursorSlice(items = emptyList(), hasNext = false)
        }

        return revenueService.getRevenueItems(authMember, contents, request)
    }

    @RequireAuth
    @GetMapping("/summary")
    @Operation(summary = "전체 수익 조회", description = "CREATOR가 등록한 작품의 전체 수익과 출금 완료 금액, 잔여 수익을 조회합니다.")
    fun getRevenueSummary(
        @AuthenticationPrincipal
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

