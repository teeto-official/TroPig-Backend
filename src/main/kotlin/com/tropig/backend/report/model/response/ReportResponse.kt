package com.tropig.backend.report.model.response

import com.tropig.backend.contents.enums.ReportType
import com.tropig.backend.report.entity.Report
import java.time.LocalDateTime

data class ReportResponse(
    val id: Long,
    val contentId: Long,
    val contentAlias: String?,
    val type: ReportType,
    val memberId: Long,
    val reporterNickname: String?,
    val reason: String,
    val resolved: Boolean,
    val createdAt: LocalDateTime,
    val contentBanned: Boolean = false,
) {
    companion object {
        fun from(
            report: Report,
            contentAlias: String? = null,
            reporterNickname: String? = null,
            contentBanned: Boolean = false,
        ) = ReportResponse(
            id = report.id,
            contentId = report.contentId,
            contentAlias = contentAlias,
            type = report.type,
            memberId = report.memberId,
            reporterNickname = reporterNickname,
            reason = report.reason,
            resolved = report.resolved,
            createdAt = report.createdAt,
            contentBanned = contentBanned,
        )
    }
}
