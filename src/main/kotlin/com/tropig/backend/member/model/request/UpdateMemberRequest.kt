package com.tropig.backend.member.model.request

import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.Rule

data class UpdateMemberRequest(
    val nickname: String?,
    val bio: String?,
    val favoriteGenres: List<Genre>,
    val favoriteRules: List<Rule>,
    val isMarketing: Boolean,
    // TODO: s3 연결하면 profile 추가
)
