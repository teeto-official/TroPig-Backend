package com.tropig.backend.payment.model.request

import jakarta.validation.constraints.NotNull

data class FailPurchaseRequest(
    @field:NotNull(message = "포트원 결제 ID는 필수입니다")
    val portonePaymentId: String,

    val code: String? = null,

    val message: String? = null,
)
