package com.tropig.backend.recruitment.model.dto

import com.tropig.backend.recruitment.enums.PlayEnvironment

data class SearchRecruitmentDto(
    val keyword: String? = null,
    val ruleIds: List<Long>? = null,
    val environments: List<PlayEnvironment>? = null,
    val page: Int = 0,
    val size: Int = 15,
)
