package com.tropig.backend.recruitment.service

import com.tropig.backend.contents.repository.ContentOptionRepository
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.service.S3Service
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.entity.RecruitmentApplication
import com.tropig.backend.recruitment.model.PersonRef
import com.tropig.backend.recruitment.model.response.ApplicationResponse
import com.tropig.backend.recruitment.model.response.MyApplicationSummaryResponse
import com.tropig.backend.recruitment.model.response.RecruitMemberResponse
import com.tropig.backend.recruitment.model.response.RecruitmentDetailResponse
import com.tropig.backend.recruitment.model.response.RecruitmentSummaryResponse
import com.tropig.backend.recruitment.model.response.RuleRefResponse
import com.tropig.backend.recruitment.model.response.ScenarioRefResponse
import com.tropig.backend.recruitment.model.response.ScheduleResponse
import com.tropig.backend.recruitment.repository.RecruitmentApplicationRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RecruitmentMapper(
    private val memberRepository: MemberRepository,
    private val contentRepository: ContentRepository,
    private val contentOptionRepository: ContentOptionRepository,
    private val applicationRepository: RecruitmentApplicationRepository,
    private val s3Service: S3Service,
) {
    fun toSummaries(recruitments: List<Recruitment>, now: LocalDateTime): List<RecruitmentSummaryResponse> {
        val ruleMap = findRuleNames(recruitments.flatMap { it.details.rules.mapNotNull { rule -> rule.ruleId } })
        val memberMap = findMembers(recruitments.map { it.writerMemberId })
        val applicationCounts = findApplicationCounts(recruitments)

        return recruitments.map { recruitment ->
            toSummary(recruitment, applicationCounts[recruitment.id] ?: 0L, ruleMap, memberMap, now)
        }
    }

    fun toDetail(
        recruitment: Recruitment,
        viewerMemberId: Long?,
        myApplication: RecruitmentApplication?,
        now: LocalDateTime,
    ): RecruitmentDetailResponse {
        val applicationCount = applicationRepository.countByRecruitmentId(recruitment.id)
        val details = recruitment.details
        val ruleMap = findRuleNames(details.rules.mapNotNull { it.ruleId })
        val scenarioMap = contentRepository
            .findAllById(details.scenarios.mapNotNull { it.scenarioId }.distinct())
            .associateBy { it.id }
        val memberIds = details.gm.mapNotNull { it.memberId } +
            details.pl.mapNotNull { it.memberId } +
            recruitment.writerMemberId
        val memberMap = findMembers(memberIds)
        val isWriter = viewerMemberId == recruitment.writerMemberId

        return RecruitmentDetailResponse(
            id = recruitment.id,
            title = recruitment.title,
            status = recruitment.effectiveStatus(now),
            deadlineAt = recruitment.deadlineAt,
            environments = details.environments,
            scenarios = details.scenarios.map { ref ->
                val content = ref.scenarioId?.let { scenarioMap[it] }
                ScenarioRefResponse(
                    scenarioId = ref.scenarioId,
                    title = content?.title,
                    alias = content?.alias,
                    text = ref.text,
                )
            },
            rules = details.rules.map { RuleRefResponse(it.ruleId, it.ruleId?.let { id -> ruleMap[id] }, it.text) },
            gm = details.gm.map { toMemberResponse(it, memberMap) },
            pl = details.pl.map { toMemberResponse(it, memberMap) },
            schedules = details.schedules.map { ScheduleResponse(it.date, it.time, it.text) },
            playTimeHours = recruitment.playTimeHours,
            playTimeText = recruitment.playTimeText,
            overview = recruitment.overview,
            caution = recruitment.caution,
            notice = recruitment.notice,
            writer = toWriterResponse(recruitment.writerMemberId, memberMap),
            applicationCount = applicationCount,
            isWriter = isWriter,
            myApplication = myApplication?.let { MyApplicationSummaryResponse(it.id, it.selected) },
            completionMessage = recruitment.completionMessage.takeIf { isWriter },
            completedAt = recruitment.completedAt,
            createdAt = recruitment.createdAt,
            updatedAt = recruitment.updatedAt,
        )
    }

    fun toApplicationResponses(applications: List<RecruitmentApplication>): List<ApplicationResponse> {
        val memberMap = findMembers(applications.map { it.applicantMemberId })

        return applications.map { application ->
            ApplicationResponse(
                id = application.id,
                recruitmentId = application.recruitmentId,
                applicant = toWriterResponse(application.applicantMemberId, memberMap),
                content = application.content,
                selected = application.selected,
                createdAt = application.createdAt,
            )
        }
    }

    private fun toSummary(
        recruitment: Recruitment,
        applicationCount: Long,
        ruleMap: Map<Long, String>,
        memberMap: Map<Long, Member>,
        now: LocalDateTime,
    ): RecruitmentSummaryResponse = RecruitmentSummaryResponse(
        id = recruitment.id,
        title = recruitment.title,
        status = recruitment.effectiveStatus(now),
        deadlineAt = recruitment.deadlineAt,
        environments = recruitment.details.environments,
        rules = recruitment.details.rules.map {
            RuleRefResponse(it.ruleId, it.ruleId?.let { id -> ruleMap[id] }, it.text)
        },
        writer = toWriterResponse(recruitment.writerMemberId, memberMap),
        gmFilled = recruitment.details.gm.isNotEmpty(),
        plCount = recruitment.details.pl.size,
        playTimeHours = recruitment.playTimeHours,
        playTimeText = recruitment.playTimeText,
        applicationCount = applicationCount,
        createdAt = recruitment.createdAt,
    )

    private fun toMemberResponse(ref: PersonRef, memberMap: Map<Long, Member>): RecruitMemberResponse {
        val member = ref.memberId?.let { memberMap[it] }

        return RecruitMemberResponse(
            memberId = ref.memberId,
            nickname = member?.nickname,
            profileUrl = s3Service.toUrl(member?.profile),
            text = ref.text,
        )
    }

    private fun toWriterResponse(memberId: Long, memberMap: Map<Long, Member>): RecruitMemberResponse {
        val member = memberMap[memberId]

        return RecruitMemberResponse(
            memberId = memberId,
            nickname = member?.nickname,
            profileUrl = s3Service.toUrl(member?.profile),
        )
    }

    private fun findMembers(ids: List<Long>): Map<Long, Member> {
        val distinctIds = ids.distinct()
        if (distinctIds.isEmpty()) return emptyMap()

        return memberRepository.findByIdInAndDeletedAtIsNull(distinctIds).associateBy { it.id }
    }

    private fun findRuleNames(ids: List<Long>): Map<Long, String> {
        val distinctIds = ids.distinct()
        if (distinctIds.isEmpty()) return emptyMap()

        return contentOptionRepository.findAllById(distinctIds).associate { it.id to it.displayName }
    }

    private fun findApplicationCounts(recruitments: List<Recruitment>): Map<Long, Long> {
        if (recruitments.isEmpty()) return emptyMap()

        return applicationRepository.countByRecruitmentIds(recruitments.map { it.id }.distinct())
            .associate { it.recruitmentId to it.count }
    }
}
