package com.tropig.backend.report.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.IllegalArgumentException
import com.tropig.backend.report.entity.Report
import com.tropig.backend.report.model.request.CreateReportRequest
import com.tropig.backend.report.model.response.ReportResponse
import com.tropig.backend.report.repository.ReportRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportService(private val reportRepository: ReportRepository) {

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
    fun getReports(): List<ReportResponse> = reportRepository.findAll().map { ReportResponse.from(it) }
}
