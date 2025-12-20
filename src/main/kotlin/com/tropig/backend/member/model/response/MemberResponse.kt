package com.tropig.backend.member.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.enums.SnsProvider

data class MemberResponse(
    val id: Long,
    val nickname: String,
    val snsProvider: SnsProvider,
    val role: Role,
    val profile: String?,
    val favoriteGenres: List<Genre>,
    val favoriteRules: List<Rule>,
    val bio: String?,
    val marketing: Boolean,
) {
    companion object {
        fun from(member: Member): MemberResponse {
            return MemberResponse(
                member.id,
                member.nickname,
                member.snsProvider,
                member.role,
                member.profile,
                Genre.fromList(member.favoriteGenres),
                Rule.fromList(member.favoriteRules),
                member.bio,
                member.marketingAt != null,
            )
        }
    }
}
