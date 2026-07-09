package com.tropig.backend.recruitment.repository

import com.tropig.backend.recruitment.entity.RecruitAlert
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RecruitAlertRepository : JpaRepository<RecruitAlert, Long>
