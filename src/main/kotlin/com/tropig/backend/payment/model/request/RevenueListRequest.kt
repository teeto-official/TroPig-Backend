package com.tropig.backend.payment.model.request

import java.time.LocalDateTime

data class RevenueListRequest(val cursorCreatedAt: LocalDateTime? = null, val cursorId: Long = 0L, val size: Int = 15)
