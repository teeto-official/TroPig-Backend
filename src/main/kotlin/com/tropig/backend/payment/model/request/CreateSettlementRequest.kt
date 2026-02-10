package com.tropig.backend.payment.model.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class CreateSettlementRequest(
    @field:NotNull(message = "정산 금액은 필수입니다")
    @field:Min(value = 1, message = "정산 금액은 1원 이상이어야 합니다")
    val settlementAmount: Long,
    
    val settlementDate: String? = null, // null이면 오늘 날짜 사용 (yyyy-MM-dd 형식)
    
    val memo: String? = null, // 정산 메모
)
