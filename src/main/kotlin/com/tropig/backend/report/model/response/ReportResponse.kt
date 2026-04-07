package com.tropig.backend.report.model.response

import com.tropig.backend.contents.enums.ReportType
import com.tropig.backend.report.entity.Report
import java.time.LocalDateTime

data class ReportResponse(
    val id: Long,
    val contentId: Long,
    val type: ReportType,
    val memberId: Long,
    val reason: String,
    val resolved: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(report: Report) = ReportResponse(
            id = report.id,
            contentId = report.contentId,
            type = report.type,
            memberId = report.memberId,
            reason = report.reason,
            resolved = report.resolved,
            createdAt = report.createdAt,
        )
    }
}
