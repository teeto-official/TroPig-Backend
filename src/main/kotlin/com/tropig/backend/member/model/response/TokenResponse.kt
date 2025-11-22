package com.tropig.backend.member.model.response

import java.time.LocalDateTime

data class TokenResponse(
    val accessToken: String,
    val resetToken: String,
    val expireAt: LocalDateTime,
)
