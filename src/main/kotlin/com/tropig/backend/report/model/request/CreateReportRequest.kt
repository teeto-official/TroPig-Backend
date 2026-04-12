package com.tropig.backend.report.model.request

import com.tropig.backend.contents.enums.ReportType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateReportRequest(
    @field:NotNull(message = "콘텐츠 ID는 필수입니다.")
    val contentId: Long,

    @field:NotNull(message = "신고 유형은 필수입니다.")
    val type: ReportType,

    @field:Size(min = 15, message = "신고 내용은 15자 이상이어야 합니다.")
    @field:NotNull(message = "신고 내용은 필수입니다.")
    val reason: String,
)
