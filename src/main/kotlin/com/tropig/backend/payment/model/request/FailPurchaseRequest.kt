package com.tropig.backend.payment.model.request

import jakarta.validation.constraints.NotNull

data class FailPurchaseRequest(
    @field:NotNull(message = "주문 ID는 필수입니다")
    val orderId: String,

    val code: String? = null,

    val message: String? = null,
)
