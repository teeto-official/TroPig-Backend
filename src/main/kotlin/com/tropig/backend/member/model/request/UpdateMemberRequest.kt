package com.tropig.backend.member.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule
import org.springframework.web.multipart.MultipartFile

data class UpdateMemberRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val favoriteGenres: List<Genre> = emptyList(),
    val favoriteRules: List<Rule> = emptyList(),
    val isMarketing: Boolean? = null,
    val profile: MultipartFile? = null,
)
