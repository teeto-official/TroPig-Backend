package com.tropig.backend.payment.model.request

import jakarta.validation.constraints.NotNull

data class ConfirmPurchaseRequest(
    @field:NotNull(message = "포트원 결제 ID는 필수입니다")
    val portonePaymentId: String,

    @field:NotNull(message = "트랜잭션 ID는 필수입니다")
    val txId: String,

    @field:NotNull(message = "결제 토큰은 필수입니다")
    val paymentToken: String,

    @field:NotNull(message = "스토어 ID는 필수입니다")
    val storeId: String,
)
