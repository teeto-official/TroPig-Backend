package com.tropig.backend.payment.model.response

data class CreatePurchaseResponse(
    val orderId: String,
    val amount: Long,
    val message: String = "결제가 생성되었습니다. 결제를 완료해주세요.",
)
