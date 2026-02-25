package com.tropig.backend.member.model.response

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.entity.MemberAuthInfo
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.enums.SnsProvider
import java.time.LocalDateTime

data class MemberResponse(
    val id: Long,
    val nickname: String,
    val snsProvider: SnsProvider,
    val email: String,
    val role: Role,
    val profile: String?,
    val favoriteGenres: List<Genre>,
    val favoriteRules: List<Rule>,
    val bio: String?,
    val isMarketing: Boolean,
    val isAuth: Boolean,
    val authDateAt: LocalDateTime?,
    val creator: Boolean,
    val authCreatorAt: LocalDateTime?,
) {
    companion object {
        fun from(member: Member, memberAuthInfo: MemberAuthInfo?): MemberResponse {
            return MemberResponse(
                member.id,
                member.nickname,
                member.snsProvider,
                member.email,
                member.role,
                member.profile,
                Genre.fromList(member.favoriteGenres),
                Rule.fromList(member.favoriteRules),
                member.bio,
                member.marketingAt != null,
                isAuth = (memberAuthInfo?.verifiedAt != null && memberAuthInfo.verifiedAt.plusDays(365) >= LocalDateTime.now()),
                authDateAt = memberAuthInfo?.verifiedAt,
                creator = memberAuthInfo?.creator ?: false,
                authCreatorAt = memberAuthInfo?.authCreatorAt
            )
        }
    }
}
