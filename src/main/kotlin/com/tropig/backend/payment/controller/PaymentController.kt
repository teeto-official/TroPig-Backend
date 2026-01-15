package com.tropig.backend.payment.controller

import com.tropig.backend.common.annotation.ApiController
import com.tropig.backend.common.annotation.LoginMember
import com.tropig.backend.common.annotation.RequireAuth
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.payment.model.request.ConfirmPurchaseRequest
import com.tropig.backend.payment.model.request.CreatePurchaseRequest
import com.tropig.backend.payment.model.response.CreatePurchaseResponse
import com.tropig.backend.payment.model.response.PurchaseResponse
import com.tropig.backend.payment.service.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@ApiController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "결제 API")
class PaymentController(
    private val paymentService: PaymentService,
) {
    @RequireAuth
    @PostMapping("/purchase")
    @Operation(summary = "구매 요청 생성", description = "콘텐츠 구매를 위한 결제를 생성합니다.")
    fun createPurchase(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: CreatePurchaseRequest
    ): ResponseEntity<CreatePurchaseResponse> {
        val response = paymentService.createPurchase(authMember.memberId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @RequireAuth
    @PostMapping("/purchase/confirm")
    @Operation(summary = "결제 승인", description = "생성된 결제를 승인합니다.")
    fun confirmPurchase(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @Valid @RequestBody request: ConfirmPurchaseRequest
    ): ResponseEntity<PurchaseResponse> {
        val response = paymentService.confirmPurchase(authMember.memberId, request)
        return ResponseEntity.ok(response)
    }

    @RequireAuth
    @GetMapping("/purchase/{purchaseId}")
    @Operation(summary = "구매 내역 조회", description = "특정 구매 내역을 조회합니다.")
    fun getPurchase(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @PathVariable purchaseId: Long
    ): ResponseEntity<PurchaseResponse> {
        val response = paymentService.getPurchase(authMember.memberId, purchaseId)
        return ResponseEntity.ok(response)
    }

    @RequireAuth
    @GetMapping("/purchase")
    @Operation(summary = "구매 내역 목록 조회", description = "회원의 모든 구매 내역을 조회합니다.")
    fun getPurchases(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember
    ): ResponseEntity<List<PurchaseResponse>> {
        val response = paymentService.getPurchasesByMember(authMember.memberId)
        return ResponseEntity.ok(response)
    }

    @RequireAuth
    @GetMapping("/content/{contentId}/purchased")
    @Operation(summary = "콘텐츠 구매 여부 확인", description = "특정 콘텐츠의 구매 여부를 확인합니다.")
    fun isContentPurchased(
        @AuthenticationPrincipal
        @LoginMember authMember: AuthMember,
        @PathVariable contentId: Long
    ): ResponseEntity<Map<String, Boolean>> {
        val purchased = paymentService.isContentPurchased(authMember.memberId, contentId)
        return ResponseEntity.ok(mapOf("purchased" to purchased))
    }
}
