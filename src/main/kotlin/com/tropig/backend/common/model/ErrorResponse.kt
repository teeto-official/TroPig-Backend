package com.tropig.backend.common.model

import com.tropig.backend.common.enums.MessageCode
import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val code: MessageCode? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
