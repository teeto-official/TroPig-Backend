package com.tropig.backend.recruitment.model.request

data class CompleteRecruitmentRequest(val applicationIds: List<Long> = emptyList(), val message: String)
