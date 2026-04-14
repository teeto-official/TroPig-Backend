package com.tropig.backend.member.model.response

import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.Role

data class MemberProfileResponse(
    val id: Long,
    val nickname: String,
    val role: Role,
    val profile: String?,
    val bio: String?,
    val favoriteGenres: List<Long>?,
    val favoriteRules: List<Long>?,
) {
    companion object {
        fun from(member: Member): MemberProfileResponse = MemberProfileResponse(
            id = member.id,
            nickname = member.nickname,
            role = member.role,
            profile = member.profile,
            bio = member.bio,
            favoriteGenres = member.favoriteGenres?.split(",")?.map { it.trim().toLong() },
            favoriteRules = member.favoriteRules?.split(",")?.map { it.trim().toLong() },
        )
    }
}
