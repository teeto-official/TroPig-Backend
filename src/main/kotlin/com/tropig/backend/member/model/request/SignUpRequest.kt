package com.tropig.backend.member.model.request

import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.enums.SnsProvider

data class SignUpRequest(
    val snsId: String,
    val snsProvider: SnsProvider,
    val email: String,
    val nickname: String?
) {
    fun toEntity(defaultNickname: String): Member {
        return Member(
            snsId = snsId,
            snsProvider = snsProvider,
            email = email,
            nickname = defaultNickname,
            role = Role.USER,
        )
    }
}
