package com.tropig.backend.payment.model.request

import jakarta.validation.constraints.NotNull

data class PaymentWebhookRequest(
    @field:NotNull(message = "결제 ID는 필수입니다")
    val paymentId: String,
    
    @field:NotNull(message = "결제 상태는 필수입니다")
    val status: String, // PAID, FAILED 등
    
    @field:NotNull(message = "결제 금액은 필수입니다")
    val amount: Long,
    
    val partnerId: String? = null, // 파트너 ID (선택)
)
