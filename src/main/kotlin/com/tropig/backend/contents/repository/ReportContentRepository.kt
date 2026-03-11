package com.tropig.backend.contents.repository

import com.tropig.backend.contents.entity.ReportContent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportContentRepository : JpaRepository<ReportContent, Long>
