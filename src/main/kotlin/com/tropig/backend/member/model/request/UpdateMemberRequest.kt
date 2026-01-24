package com.tropig.backend.member.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule

data class UpdateMemberRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val favoriteGenres: List<Genre> = emptyList(),
    val favoriteRules: List<Rule> = emptyList(),
    val isMarketing: Boolean? = null,
    // TODO: s3 연결하면 profile 추가
)
