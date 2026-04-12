package com.tropig.backend.report.model.response

import com.tropig.backend.contents.enums.ReportType

data class ReportTypeResponse(val type: String, val label: String) {
    companion object {
        fun fromAll(): List<ReportTypeResponse> = listOf(
            ReportTypeResponse(ReportType.SPAM.name, "스팸"),
            ReportTypeResponse(ReportType.INAPPROPRIATE.name, "부적절한 내용"),
            ReportTypeResponse(ReportType.COPYRIGHT.name, "저작권 침해"),
            ReportTypeResponse(ReportType.HARASSMENT.name, "괴롭힘"),
            ReportTypeResponse(ReportType.ETC.name, "기타"),
        )
    }
}
