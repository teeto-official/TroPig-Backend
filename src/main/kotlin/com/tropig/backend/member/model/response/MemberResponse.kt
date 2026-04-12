package com.tropig.backend.member.model.response

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
    val favoriteGenres: List<Long>?,
    val favoriteRules: List<Long>?,
    val bio: String?,
    val isMarketing: Boolean,
    val isAuth: Boolean,
    val authDateAt: LocalDateTime?,
    val creator: Boolean,
    val authCreatorAt: LocalDateTime?,
    val name: String?,
    val phoneNumber: String?,
) {
    companion object {
        fun from(member: Member, memberAuthInfo: MemberAuthInfo?): MemberResponse = MemberResponse(
            id = member.id,
            nickname = member.nickname,
            snsProvider = member.snsProvider,
            email = member.email,
            role = member.role,
            profile = member.profile,
            favoriteGenres = member.favoriteGenres?.split(",")?.map { it.toLong() },
            favoriteRules = member.favoriteRules?.split(",")?.map { it.toLong() },
            bio = member.bio,
            isMarketing = member.marketingAt != null,
            isAuth = (
                memberAuthInfo?.verifiedAt != null &&
                    memberAuthInfo.verifiedAt.plusDays(365) >= LocalDateTime.now()
                ),
            authDateAt = memberAuthInfo?.verifiedAt,
            creator = memberAuthInfo?.creator ?: false,
            authCreatorAt = memberAuthInfo?.authCreatorAt,
            name = memberAuthInfo?.name,
            phoneNumber = memberAuthInfo?.phoneNumber,
        )
    }
}
