package com.tropig.backend.payment.model.response

import com.tropig.backend.payment.enums.WithdrawalStatus
import java.time.LocalDateTime

data class WithdrawalItemResponse(
    val amount: Long,
    val createdAt: LocalDateTime,
    val withdrawalStatus: WithdrawalStatus,
)
