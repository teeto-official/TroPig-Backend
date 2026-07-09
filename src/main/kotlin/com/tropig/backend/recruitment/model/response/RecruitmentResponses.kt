package com.tropig.backend.recruitment.model.response

import com.tropig.backend.recruitment.enums.PlayEnvironment
import com.tropig.backend.recruitment.enums.RecruitmentBadge
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import java.time.LocalDateTime

data class RecruitMemberResponse(
    val memberId: Long? = null,
    val nickname: String? = null,
    val profileUrl: String? = null,
    val text: String? = null,
)

data class ScenarioRefResponse(
    val scenarioId: Long? = null,
    val title: String? = null,
    val alias: String? = null,
    val text: String? = null,
)

data class RuleRefResponse(val ruleId: Long? = null, val displayName: String? = null, val text: String? = null)

data class ScheduleResponse(val date: String? = null, val time: String? = null, val text: String? = null)

data class RecruitmentSummaryResponse(
    val id: Long,
    val title: String,
    val status: RecruitmentStatus,
    val deadlineAt: LocalDateTime,
    val environments: List<PlayEnvironment>,
    val rules: List<RuleRefResponse>,
    val writer: RecruitMemberResponse,
    val gmFilled: Boolean,
    val plCount: Int,
    val playTimeHours: Int?,
    val playTimeText: String?,
    val applicationCount: Long,
    val createdAt: LocalDateTime,
)

data class MyApplicationSummaryResponse(val applicationId: Long, val selected: Boolean)

data class RecruitmentDetailResponse(
    val id: Long,
    val title: String,
    val status: RecruitmentStatus,
    val deadlineAt: LocalDateTime,
    val environments: List<PlayEnvironment>,
    val scenarios: List<ScenarioRefResponse>,
    val rules: List<RuleRefResponse>,
    val gm: List<RecruitMemberResponse>,
    val pl: List<RecruitMemberResponse>,
    val schedules: List<ScheduleResponse>,
    val playTimeHours: Int?,
    val playTimeText: String?,
    val overview: String?,
    val caution: String?,
    val notice: String?,
    val writer: RecruitMemberResponse,
    val applicationCount: Long,
    val isWriter: Boolean,
    val myApplication: MyApplicationSummaryResponse?,
    val completionMessage: String?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ApplicationResponse(
    val id: Long,
    val recruitmentId: Long,
    val applicant: RecruitMemberResponse,
    val content: String,
    val selected: Boolean,
    val createdAt: LocalDateTime,
)

data class MyApplicationResponse(
    val applicationId: Long,
    val content: String,
    val badge: RecruitmentBadge,
    val message: String?,
    val recruitment: RecruitmentSummaryResponse,
    val appliedAt: LocalDateTime,
)

data class RecruitAlertResponse(val hosting: Boolean, val applied: Boolean)
