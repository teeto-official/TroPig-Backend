package com.tropig.backend.payment.model.request

import com.tropig.backend.payment.enums.PaymentChannel
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class CreatePurchaseRequest(
    @field:NotNull(message = "콘텐츠 ID는 필수입니다")
    val contentId: Long,
    
    @field:NotNull(message = "채널 키는 필수입니다")
    val channel: PaymentChannel,
)
