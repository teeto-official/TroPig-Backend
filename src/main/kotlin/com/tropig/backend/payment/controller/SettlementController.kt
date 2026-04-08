package com.tropig.backend.payment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.payment.model.request.CreateSettlementRequest
import com.tropig.backend.payment.model.response.RevenueSummaryResponse
import com.tropig.backend.payment.model.response.SettlementResponse
import com.tropig.backend.payment.service.SettlementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/settlement")
@Tag(name = "Settlement", description = "정산 API")
class SettlementController(private val settlementService: SettlementService) {

    @RequireAuth
    @PostMapping
    @Operation(summary = "정산 요청", description = "파트너 정산을 요청합니다.")
    fun createSettlement(
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: CreateSettlementRequest,
    ): ResponseEntity<SettlementResponse> {
        val response = settlementService.createSettlement(authMember, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @RequireAuth
    @GetMapping("/available")
    @Operation(summary = "정산 가능 금액 조회", description = "현재 정산 가능한 금액을 조회합니다.")
    fun getAvailableSettlementAmount(@LoginMember authMember: AuthMember): ResponseEntity<RevenueSummaryResponse> {
        val response = settlementService.getAvailableSettlementAmount(authMember)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{settlementId}/complete")
    @Operation(summary = "정산 완료 처리", description = "정산 상태를 COMPLETED로 변경합니다.")
    fun completeSettlement(@PathVariable settlementId: Long): ResponseEntity<SettlementResponse> {
        val response = settlementService.completeSettlement(settlementId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{settlementId}/fail")
    @Operation(summary = "정산 실패 처리", description = "정산 상태를 FAILED로 변경합니다.")
    fun failSettlement(
        @PathVariable settlementId: Long,
        @RequestParam(required = false) reason: String?,
    ): ResponseEntity<SettlementResponse> {
        val response = settlementService.failSettlement(settlementId, reason)
        return ResponseEntity.ok(response)
    }
}
