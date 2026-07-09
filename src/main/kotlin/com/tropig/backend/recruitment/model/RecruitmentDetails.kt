package com.tropig.backend.recruitment.model

import com.tropig.backend.recruitment.enums.PlayEnvironment

data class RecruitmentDetails(
    val scenarios: List<ScenarioRef> = emptyList(),
    val rules: List<RuleRef> = emptyList(),
    val gm: List<PersonRef> = emptyList(),
    val pl: List<PersonRef> = emptyList(),
    val schedules: List<ScheduleItem> = emptyList(),
    val environments: List<PlayEnvironment> = emptyList(),
)

data class ScenarioRef(val scenarioId: Long? = null, val text: String? = null)

data class RuleRef(val ruleId: Long? = null, val text: String? = null)

data class PersonRef(val memberId: Long? = null, val text: String? = null)

data class ScheduleItem(val date: String? = null, val time: String? = null, val text: String? = null)
