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
        if (request.reason.length < 15) {
            throw IllegalArgumentException("신고 내용은 15자 이상이어야 합니다.", MessageCode.INVALID_PARAMS)
        }
        val report = Report(
            contentId = request.contentId,
            type = request.type,
            memberId = memberId,
            reason = request.reason,
        )
        return ReportResponse.from(reportRepository.save(report))
    }

    @Transactional(readOnly = true)
    fun getReports(): List<ReportResponse> {
        val reports = reportRepository.findAll()
        val contentIds = reports.map { it.contentId }.distinct()
        val memberIds = reports.map { it.memberId }.distinct()
        val aliasById: Map<Long, String> = contentRepository.findAliasesByIds(contentIds)
            .associate { it.id to it.alias }
        val nicknameById: Map<Long, String> = memberRepository.findNicknamesByIds(memberIds)
            .associate { it.id to it.nickname }
        return reports.map { ReportResponse.from(it, aliasById[it.contentId], nicknameById[it.memberId]) }
    }

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
        val report = reportRepository.findByMemberIdAndContentId(memberId, contentId)
        return if (report == null) {
            MyReportResponse(reported = false, resolved = false)
        } else {
            MyReportResponse(reported = true, resolved = report.resolved)
        }
    }
}
