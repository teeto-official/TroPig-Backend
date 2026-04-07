package com.tropig.backend.report.repository

import com.tropig.backend.report.entity.Report
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportRepository : JpaRepository<Report, Long> {
    fun findByMemberIdAndContentId(memberId: Long, contentId: Long): Report?
}
