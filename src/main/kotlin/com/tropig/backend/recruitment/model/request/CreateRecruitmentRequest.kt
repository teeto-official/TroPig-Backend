package com.tropig.backend.recruitment.model.request

import com.tropig.backend.recruitment.enums.PlayEnvironment
import java.time.LocalDateTime

data class CreateRecruitmentRequest(
    val title: String,
    val scenarios: List<ScenarioRefRequest> = emptyList(),
    val rules: List<RuleRefRequest> = emptyList(),
    val gm: List<PersonRefRequest> = emptyList(),
    val pl: List<PersonRefRequest> = emptyList(),
    val deadlineAt: LocalDateTime,
    val schedules: List<ScheduleRequest> = emptyList(),
    val playTimeHours: Int? = null,
    val playTimeText: String? = null,
    val environments: List<PlayEnvironment> = emptyList(),
    val overview: String? = null,
    val caution: String? = null,
    val notice: String? = null,
)

data class ScenarioRefRequest(val scenarioId: Long? = null, val text: String? = null)

data class RuleRefRequest(val ruleId: Long? = null, val text: String? = null)

data class PersonRefRequest(val memberId: Long? = null, val text: String? = null)

data class ScheduleRequest(val date: String? = null, val time: String? = null, val text: String? = null)
