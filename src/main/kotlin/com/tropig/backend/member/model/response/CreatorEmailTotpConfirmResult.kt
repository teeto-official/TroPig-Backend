package com.tropig.backend.member.model.response

import com.tropig.backend.member.enums.Role
import java.time.LocalDateTime

data class CreatorEmailTotpConfirmResult(
    val verified: Boolean,
    val role: Role,
    val verifiedAt: LocalDateTime,
    val message: String,
)
