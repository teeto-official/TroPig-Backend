package com.tropig.backend.payment.model.response

data class RevenueSummaryResponse(val totalRevenue: Long, val withdrawnAmount: Long, val availableRevenue: Long)
