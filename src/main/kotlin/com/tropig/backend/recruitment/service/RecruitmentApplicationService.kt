package com.tropig.backend.recruitment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.recruitment.RecruitmentPolicy
import com.tropig.backend.recruitment.entity.RecruitmentApplication
import com.tropig.backend.recruitment.enums.RecruitmentBadge
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.model.request.CreateApplicationRequest
import com.tropig.backend.recruitment.model.response.ApplicationResponse
import com.tropig.backend.recruitment.model.response.MyApplicationResponse
import com.tropig.backend.recruitment.repository.RecruitmentApplicationRepository
import com.tropig.backend.recruitment.repository.RecruitmentRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import com.tropig.backend.common.exception.IllegalArgumentException as TroPigIllegalArgumentException

@Service
class RecruitmentApplicationService(
    private val recruitmentRepository: RecruitmentRepository,
    private val applicationRepository: RecruitmentApplicationRepository,
    private val accessGuard: RecruitmentAccessGuard,
    private val mapper: RecruitmentMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RecruitmentApplicationService::class.java)
    }

    @Transactional
    fun apply(recruitmentId: Long, memberId: Long, request: CreateApplicationRequest): ApplicationResponse {
        val recruitment = accessGuard.findVisible(recruitmentId)

        if (recruitment.writerMemberId == memberId) {
            throw TroPigIllegalArgumentException(
                "본인이 작성한 구인글에는 신청할 수 없습니다.",
                MessageCode.CANNOT_APPLY_OWN_RECRUITMENT,
            )
        }
        if (recruitment.effectiveStatus(LocalDateTime.now()) != RecruitmentStatus.RECRUITING) {
            throw TroPigIllegalArgumentException("모집중인 구인글이 아닙니다.", MessageCode.NOT_RECRUITING)
        }
        if (request.content.isBlank()) {
            throw TroPigIllegalArgumentException("신청 내용을 입력해주세요.", MessageCode.INVALID_PARAMS)
        }
        if (applicationRepository.existsByRecruitmentIdAndApplicantMemberId(recruitmentId, memberId)) {
            throw TroPigIllegalArgumentException("이미 신청한 구인글입니다.", MessageCode.ALREADY_APPLIED)
        }

        val application = try {
            applicationRepository.save(
                RecruitmentApplication(
                    recruitmentId = recruitmentId,
                    applicantMemberId = memberId,
                    content = request.content.trim(),
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            logger.warn("구인 신청 중복 저장 감지: recruitmentId=$recruitmentId, memberId=$memberId", e)
            throw TroPigIllegalArgumentException("이미 신청한 구인글입니다.", MessageCode.ALREADY_APPLIED)
        }

        return mapper.toApplicationResponses(listOf(application)).first()
    }

    @Transactional(readOnly = true)
    fun getApplications(recruitmentId: Long, memberId: Long): List<ApplicationResponse> {
        val recruitment = accessGuard.findVisible(recruitmentId)
        accessGuard.checkWriter(recruitment, memberId)

        return mapper.toApplicationResponses(applicationRepository.findAllByRecruitmentIdOrderByIdAsc(recruitmentId))
    }

    @Transactional(readOnly = true)
    fun getApplication(applicationId: Long, memberId: Long): ApplicationResponse {
        val application = applicationRepository.findById(applicationId).orElse(null)
            ?: throw NotFoundException("신청 내역을 찾을 수 없습니다. id: $applicationId", MessageCode.NOT_FOUND_APPLICATION)
        val recruitment = recruitmentRepository.findById(application.recruitmentId).orElse(null)

        val isWriter = recruitment?.writerMemberId == memberId
        val isApplicant = application.applicantMemberId == memberId
        if (!isWriter && !isApplicant) {
            throw NotFoundException("신청 내역을 찾을 수 없습니다. id: $applicationId", MessageCode.NOT_FOUND_APPLICATION)
        }

        return mapper.toApplicationResponses(listOf(application)).first()
    }

    @Transactional(readOnly = true)
    fun getMyApplications(memberId: Long, page: Int): Page<MyApplicationResponse> {
        val pageable = PageRequest.of(page, RecruitmentPolicy.PAGE_SIZE, Sort.by("id").descending())
        val applications = applicationRepository.findAllByApplicantMemberId(memberId, pageable)
        val recruitments = recruitmentRepository
            .findAllById(applications.content.map { it.recruitmentId }.distinct())
            .associateBy { it.id }
        val now = LocalDateTime.now()
        val summaries = mapper.toSummaries(recruitments.values.toList(), now).associateBy { it.id }

        val items = applications.content.mapNotNull { application ->
            val recruitment = recruitments[application.recruitmentId] ?: return@mapNotNull null
            val summary = summaries[application.recruitmentId] ?: return@mapNotNull null
            val badge = RecruitmentBadge.of(recruitment.effectiveStatus(now), application.selected)

            MyApplicationResponse(
                applicationId = application.id,
                content = application.content,
                badge = badge,
                message = recruitment.completionMessage.takeIf { badge == RecruitmentBadge.SELECTED },
                recruitment = summary,
                appliedAt = application.createdAt,
            )
        }

        return PageImpl(items, applications.pageable, applications.totalElements)
    }
}
