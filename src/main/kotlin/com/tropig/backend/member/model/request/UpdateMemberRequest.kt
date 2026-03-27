package com.tropig.backend.member.model.request

import org.springframework.web.multipart.MultipartFile

data class UpdateMemberRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val favoriteGenres: List<Long> = emptyList(),
    val favoriteRules: List<Long> = emptyList(),
    val isMarketing: Boolean? = null,
    val profile: MultipartFile? = null,
)
