package com.tropig.backend.common.model

import com.tropig.backend.member.enums.Role

data class AuthMember(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val adult: Boolean,
    val role: Role,
)
