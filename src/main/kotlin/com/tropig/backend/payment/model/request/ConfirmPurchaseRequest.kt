package com.tropig.backend.payment.model.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotNull

@JsonIgnoreProperties(ignoreUnknown = true)
data class ConfirmPurchaseRequest(
    @field:NotNull(message = "주문 ID는 필수입니다")
    val orderId: String,

    @field:NotNull(message = "결제 키는 필수입니다")
    val paymentKey: String,

    @field:NotNull(message = "결제 금액은 필수입니다")
    val amount: Double,
) {
    fun amountAsLong(): Long = amount.toLong()
}
