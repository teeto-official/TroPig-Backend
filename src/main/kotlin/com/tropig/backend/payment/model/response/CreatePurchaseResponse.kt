package com.tropig.backend.payment.model.response

data class CreatePurchaseResponse(
    val paymentId: Long,
    val portonePaymentId: String, // Payment 엔티티의 portonePaymentId (프론트에서 결제 승인 시 필요)
    val amount: Long,
    val message: String = "결제가 생성되었습니다. 결제를 완료해주세요."
)
