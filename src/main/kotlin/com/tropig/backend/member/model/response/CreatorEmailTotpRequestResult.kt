package com.tropig.backend.member.model.response

import java.time.LocalDateTime

data class CreatorEmailTotpRequestResult(
    val verificationId: String,
    val email: String,
    val expiresAt: LocalDateTime,
    val retryAvailableAt: LocalDateTime,
    val message: String,
)
