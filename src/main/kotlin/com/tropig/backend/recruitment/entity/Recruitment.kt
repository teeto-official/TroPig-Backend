package com.tropig.backend.recruitment.entity

import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.model.RecruitmentDetails
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "recruitment")
data class Recruitment(
    var writerMemberId: Long,
    var title: String,
    @Enumerated(value = EnumType.STRING)
    var status: RecruitmentStatus,
    var deadlineAt: LocalDateTime,
    var playTimeHours: Int?,
    var playTimeText: String?,
    @Column(columnDefinition = "TEXT")
    var overview: String?,
    @Column(columnDefinition = "TEXT")
    var caution: String?,
    @Column(columnDefinition = "TEXT")
    var notice: String?,
    @JdbcTypeCode(SqlTypes.JSON)
    var details: RecruitmentDetails,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Column(columnDefinition = "TEXT")
    var completionMessage: String? = null

    var completedAt: LocalDateTime? = null

    val createdAt: LocalDateTime = LocalDateTime.now()

    var updatedAt: LocalDateTime = LocalDateTime.now()

    var deletedAt: LocalDateTime? = null

    fun effectiveStatus(now: LocalDateTime): RecruitmentStatus =
        if (status == RecruitmentStatus.RECRUITING && !deadlineAt.isAfter(now)) {
            RecruitmentStatus.CLOSED
        } else {
            status
        }
}
