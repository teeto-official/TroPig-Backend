package com.tropig.backend.report.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.IllegalArgumentException
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.report.entity.Report
import com.tropig.backend.report.model.request.CreateReportRequest
import com.tropig.backend.report.model.response.MyReportResponse
import com.tropig.backend.report.model.response.ReportResponse
import com.tropig.backend.report.model.response.ReportTypeResponse
import com.tropig.backend.report.repository.ReportRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val contentRepository: ContentRepository,
    private val memberRepository: MemberRepository,
) {

    @Transactional
    fun createReport(memberId: Long, request: CreateReportRequest): ReportResponse {
        val report = Report(
            contentId = request.contentId,
            type = request.type,
            memberId = memberId,
            reason = request.reason,
        )
        return ReportResponse.from(reportRepository.save(report))
    }

    @Transactional(readOnly = true)
    fun getReports(pageable: Pageable): Page<ReportResponse> {
        val reportPage = reportRepository.findAll(pageable)
        val reports = reportPage.content
        val contentIds = reports.map { it.contentId }.distinct()
        val memberIds = reports.map { it.memberId }.distinct()
        val aliasById = contentRepository.findByIdIn(contentIds).associate { it.id to it.alias }
        val nicknameById = memberRepository.findByIdInAndDeletedAtIsNull(memberIds).associate { it.id to it.nickname }
        return reportPage.map { ReportResponse.from(it, aliasById[it.contentId], nicknameById[it.memberId]) }
    }

    @Cacheable(cacheNames = ["report:types"])
    fun getReportTypes(): List<ReportTypeResponse> = ReportTypeResponse.fromAll()

    @Transactional
    fun resolveReport(id: Long): ReportResponse {
        val report = reportRepository.findById(id)
            .orElseThrow { IllegalArgumentException("존재하지 않는 신고입니다.", MessageCode.INVALID_PARAMS) }
        report.resolved = true
        val saved = reportRepository.save(report)
        val alias = contentRepository.findById(saved.contentId).orElse(null)?.alias
        val nickname = memberRepository.findMemberByIdAndDeletedAtIsNull(saved.memberId)?.nickname
        return ReportResponse.from(saved, alias, nickname)
    }

    @Transactional(readOnly = true)
    fun getMyReport(memberId: Long, contentId: Long): MyReportResponse {
        val report = reportRepository.findTopByMemberIdAndContentIdOrderByCreatedAtDesc(memberId, contentId)
        val canReport = report == null || report.resolved
        return MyReportResponse(canReport = canReport)
    }
}
