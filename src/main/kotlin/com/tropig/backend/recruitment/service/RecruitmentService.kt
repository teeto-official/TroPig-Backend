package com.tropig.backend.recruitment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.OptionType
import com.tropig.backend.contents.repository.ContentOptionRepository
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.recruitment.RecruitmentPolicy
import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.model.PersonRef
import com.tropig.backend.recruitment.model.RecruitmentDetails
import com.tropig.backend.recruitment.model.RuleRef
import com.tropig.backend.recruitment.model.ScenarioRef
import com.tropig.backend.recruitment.model.ScheduleItem
import com.tropig.backend.recruitment.model.dto.SearchRecruitmentDto
import com.tropig.backend.recruitment.model.request.CompleteRecruitmentRequest
import com.tropig.backend.recruitment.model.request.CreateRecruitmentRequest
import com.tropig.backend.recruitment.model.request.PersonRefRequest
import com.tropig.backend.recruitment.model.response.RecruitmentDetailResponse
import com.tropig.backend.recruitment.model.response.RecruitmentSummaryResponse
import com.tropig.backend.recruitment.repository.RecruitmentApplicationRepository
import com.tropig.backend.recruitment.repository.RecruitmentRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import com.tropig.backend.common.exception.IllegalArgumentException as TroPigIllegalArgumentException

@Service
class RecruitmentService(
    private val recruitmentRepository: RecruitmentRepository,
    private val applicationRepository: RecruitmentApplicationRepository,
    private val memberRepository: MemberRepository,
    private val contentRepository: ContentRepository,
    private val contentOptionRepository: ContentOptionRepository,
    private val accessGuard: RecruitmentAccessGuard,
    private val mapper: RecruitmentMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RecruitmentService::class.java)
    }

    @Transactional(readOnly = true)
    fun searchRecruitments(request: SearchRecruitmentDto): Page<RecruitmentSummaryResponse> {
        val page = recruitmentRepository.searchRecruitments(request)
        val summaries = mapper.toSummaries(page.content, LocalDateTime.now())

        return PageImpl(summaries, page.pageable, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun getRecruitment(id: Long, viewerMemberId: Long?): RecruitmentDetailResponse {
        val recruitment = accessGuard.findVisible(id)
        val myApplication = viewerMemberId?.let {
            applicationRepository.findByRecruitmentIdAndApplicantMemberId(id, it)
        }

        return mapper.toDetail(recruitment, viewerMemberId, myApplication, LocalDateTime.now())
    }

    @Transactional
    fun createRecruitment(memberId: Long, request: CreateRecruitmentRequest): RecruitmentDetailResponse {
        validateRequest(request)

        val recruitment = Recruitment(
            writerMemberId = memberId,
            title = request.title.trim(),
            status = RecruitmentStatus.RECRUITING,
            deadlineAt = request.deadlineAt,
            playTimeHours = request.playTimeHours,
            playTimeText = request.playTimeText?.takeUnless { it.isBlank() },
            overview = request.overview?.takeUnless { it.isBlank() },
            caution = request.caution?.takeUnless { it.isBlank() },
            notice = request.notice?.takeUnless { it.isBlank() },
            details = request.toDetails(),
        )

        val saved = recruitmentRepository.save(recruitment)
        logger.info("구인글 등록: id=${saved.id}, writerMemberId=$memberId")

        return mapper.toDetail(saved, memberId, null, LocalDateTime.now())
    }

    @Transactional
    fun updateRecruitment(id: Long, memberId: Long, request: CreateRecruitmentRequest): RecruitmentDetailResponse {
        val recruitment = accessGuard.findVisibleForUpdate(id)
        accessGuard.checkWriter(recruitment, memberId)
        validateRequest(request)

        recruitment.title = request.title.trim()
        recruitment.deadlineAt = request.deadlineAt
        recruitment.playTimeHours = request.playTimeHours
        recruitment.playTimeText = request.playTimeText?.takeUnless { it.isBlank() }
        recruitment.overview = request.overview?.takeUnless { it.isBlank() }
        recruitment.caution = request.caution?.takeUnless { it.isBlank() }
        recruitment.notice = request.notice?.takeUnless { it.isBlank() }
        recruitment.details = request.toDetails()
        recruitment.updatedAt = LocalDateTime.now()

        val saved = recruitmentRepository.save(recruitment)
        val myApplication = applicationRepository.findByRecruitmentIdAndApplicantMemberId(id, memberId)

        return mapper.toDetail(saved, memberId, myApplication, LocalDateTime.now())
    }

    @Transactional
    fun deleteRecruitment(id: Long, memberId: Long) {
        val recruitment = accessGuard.findVisibleForUpdate(id)
        accessGuard.checkWriter(recruitment, memberId)

        val now = LocalDateTime.now()
        recruitment.status = RecruitmentStatus.DELETED
        recruitment.deletedAt = now
        recruitment.updatedAt = now
        recruitmentRepository.save(recruitment)

        logger.info("구인글 삭제: id=$id, writerMemberId=$memberId")
    }

    @Transactional
    fun completeRecruitment(id: Long, memberId: Long, request: CompleteRecruitmentRequest): RecruitmentDetailResponse {
        val recruitment = accessGuard.findVisibleForUpdate(id)
        accessGuard.checkWriter(recruitment, memberId)

        if (recruitment.status == RecruitmentStatus.RECRUITMENT_COMPLETED) {
            throw TroPigIllegalArgumentException("이미 모집완료된 구인글입니다.", MessageCode.RECRUITMENT_ALREADY_COMPLETED)
        }
        if (request.message.isBlank()) {
            throwInvalid("선정 메시지를 입력해주세요.")
        }
        val applicationIds = request.applicationIds.distinct()
        if (applicationIds.isEmpty()) {
            throwInvalid("선정할 신청자를 선택해주세요.")
        }

        val applications = applicationRepository.findAllByIdInAndRecruitmentId(applicationIds, id)
        if (applications.size != applicationIds.size) {
            throw TroPigIllegalArgumentException("유효하지 않은 신청 건이 포함되어 있습니다.", MessageCode.INVALID_APPLICATION)
        }

        val now = LocalDateTime.now()
        applications.forEach { application ->
            application.selected = true
            application.updatedAt = now
        }
        applicationRepository.saveAll(applications)

        recruitment.status = RecruitmentStatus.RECRUITMENT_COMPLETED
        recruitment.completionMessage = request.message
        recruitment.completedAt = now
        recruitment.updatedAt = now
        val saved = recruitmentRepository.save(recruitment)

        logger.info("구인 완료 처리: id=$id, 선정 인원=${applications.size}")

        return mapper.toDetail(saved, memberId, null, now)
    }

    @Transactional(readOnly = true)
    fun getMyHostingRecruitments(memberId: Long, page: Int): Page<RecruitmentSummaryResponse> {
        val pageable = PageRequest.of(page, RecruitmentPolicy.PAGE_SIZE, Sort.by("id").descending())
        val recruitments = recruitmentRepository.findAllByWriterMemberIdAndDeletedAtIsNull(memberId, pageable)
        val summaries = mapper.toSummaries(recruitments.content, LocalDateTime.now())

        return PageImpl(summaries, recruitments.pageable, recruitments.totalElements)
    }

    private fun validateRequest(request: CreateRecruitmentRequest) {
        if (request.title.isBlank()) {
            throwInvalid("제목을 입력해주세요.")
        }
        if (!request.deadlineAt.isAfter(LocalDateTime.now())) {
            throwInvalid("모집 마감일시는 현재 이후여야 합니다.")
        }
        if (request.rules.isEmpty()) {
            throwInvalid("룰을 1개 이상 입력해주세요.")
        }
        if (request.environments.isEmpty()) {
            throwInvalid("플레이 환경을 1개 이상 선택해주세요.")
        }
        request.rules.forEach { rule ->
            if ((rule.ruleId == null) == rule.text.isNullOrBlank()) {
                throwInvalid("룰은 등록된 룰 선택 또는 직접 입력 중 하나여야 합니다.")
            }
        }
        request.scenarios.forEach { scenario ->
            if ((scenario.scenarioId == null) == scenario.text.isNullOrBlank()) {
                throwInvalid("시나리오는 등록된 시나리오 선택 또는 직접 입력 중 하나여야 합니다.")
            }
        }
        validatePersonRefs(request.gm, "GM")
        validatePersonRefs(request.pl, "PL")
        validateRuleIds(request.rules.mapNotNull { it.ruleId })
        validateScenarioIds(request.scenarios.mapNotNull { it.scenarioId })
        validateMemberIds((request.gm + request.pl).mapNotNull { it.memberId })
    }

    private fun validatePersonRefs(refs: List<PersonRefRequest>, fieldName: String) {
        refs.forEach { ref ->
            if ((ref.memberId == null) == ref.text.isNullOrBlank()) {
                throwInvalid("$fieldName 은(는) 회원 선택 또는 직접 입력 중 하나여야 합니다.")
            }
        }
    }

    private fun validateRuleIds(ruleIds: List<Long>) {
        val distinctIds = ruleIds.distinct()
        if (distinctIds.isEmpty()) return

        val options = contentOptionRepository.findAllById(distinctIds)
        if (options.size != distinctIds.size || options.any { it.type != OptionType.RULE }) {
            throwInvalid("존재하지 않는 룰이 포함되어 있습니다.")
        }
    }

    private fun validateScenarioIds(scenarioIds: List<Long>) {
        val distinctIds = scenarioIds.distinct()
        if (distinctIds.isEmpty()) return

        if (contentRepository.findAllById(distinctIds).size != distinctIds.size) {
            throwInvalid("존재하지 않는 시나리오가 포함되어 있습니다.")
        }
    }

    private fun validateMemberIds(memberIds: List<Long>) {
        val distinctIds = memberIds.distinct()
        if (distinctIds.isEmpty()) return

        if (memberRepository.findByIdInAndDeletedAtIsNull(distinctIds).size != distinctIds.size) {
            throwInvalid("존재하지 않는 회원이 포함되어 있습니다.")
        }
    }

    private fun CreateRecruitmentRequest.toDetails(): RecruitmentDetails = RecruitmentDetails(
        scenarios = scenarios.map {
            ScenarioRef(it.scenarioId, it.text?.trim()?.takeUnless { text -> text.isBlank() })
        },
        rules = rules.map { RuleRef(it.ruleId, it.text?.trim()?.takeUnless { text -> text.isBlank() }) },
        gm = gm.map { PersonRef(it.memberId, it.text?.trim()?.takeUnless { text -> text.isBlank() }) },
        pl = pl.map { PersonRef(it.memberId, it.text?.trim()?.takeUnless { text -> text.isBlank() }) },
        schedules = schedules
            .filter { it.date != null || it.time != null || !it.text.isNullOrBlank() }
            .map { ScheduleItem(it.date, it.time, it.text?.trim()?.takeUnless { text -> text.isBlank() }) },
        environments = environments.distinct(),
    )

    private fun throwInvalid(message: String): Nothing = throw TroPigIllegalArgumentException(
        message,
        MessageCode.INVALID_PARAMS,
    )
}
