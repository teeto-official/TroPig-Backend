package com.tropig.backend.recruitment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "recruitment_application")
data class RecruitmentApplication(
    var recruitmentId: Long,
    var applicantMemberId: Long,
    @Column(columnDefinition = "TEXT")
    var content: String,
    var selected: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    val createdAt: LocalDateTime = LocalDateTime.now()

    var updatedAt: LocalDateTime = LocalDateTime.now()
}
