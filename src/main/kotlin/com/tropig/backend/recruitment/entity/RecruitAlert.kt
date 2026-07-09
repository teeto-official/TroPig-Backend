package com.tropig.backend.recruitment.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "recruit_alert")
data class RecruitAlert(
    @Id
    val memberId: Long,
    var lastCheckedHostingAt: LocalDateTime? = null,
    var lastCheckedAppliedAt: LocalDateTime? = null,
) {
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
