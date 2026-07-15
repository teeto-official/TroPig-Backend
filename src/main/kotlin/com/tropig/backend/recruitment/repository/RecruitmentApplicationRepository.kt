package com.tropig.backend.recruitment.repository

import com.tropig.backend.recruitment.entity.RecruitmentApplication
import com.tropig.backend.recruitment.model.result.RecruitmentApplicationCount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RecruitmentApplicationRepository : JpaRepository<RecruitmentApplication, Long> {
    fun findAllByRecruitmentIdOrderByIdAsc(recruitmentId: Long): List<RecruitmentApplication>

    fun findAllByApplicantMemberId(applicantMemberId: Long, pageable: Pageable): Page<RecruitmentApplication>

    fun findAllByIdInAndRecruitmentId(ids: List<Long>, recruitmentId: Long): List<RecruitmentApplication>

    fun existsByRecruitmentIdAndApplicantMemberId(recruitmentId: Long, applicantMemberId: Long): Boolean

    fun findByRecruitmentIdAndApplicantMemberId(recruitmentId: Long, applicantMemberId: Long): RecruitmentApplication?

    fun countByRecruitmentId(recruitmentId: Long): Long

    @Query(
        """
        SELECT new com.tropig.backend.recruitment.model.result.RecruitmentApplicationCount(a.recruitmentId, COUNT(a))
        FROM RecruitmentApplication a
        WHERE a.recruitmentId IN :recruitmentIds
        GROUP BY a.recruitmentId
        """,
    )
    fun countByRecruitmentIds(@Param("recruitmentIds") recruitmentIds: List<Long>): List<RecruitmentApplicationCount>

    @Query(
        """
        SELECT COUNT(a)
        FROM RecruitmentApplication a
        JOIN Recruitment r ON a.recruitmentId = r.id
        WHERE r.writerMemberId = :memberId AND r.deletedAt IS NULL AND a.createdAt > :since
        """,
    )
    fun countUnreadHostingEvents(@Param("memberId") memberId: Long, @Param("since") since: LocalDateTime): Long

    @Query(
        """
        SELECT COUNT(a)
        FROM RecruitmentApplication a
        JOIN Recruitment r ON a.recruitmentId = r.id
        WHERE a.applicantMemberId = :memberId
        AND a.selected = true
        AND r.deletedAt IS NULL
        AND r.completedAt IS NOT NULL
        AND r.completedAt > :since
        """,
    )
    fun countUnreadAppliedEvents(@Param("memberId") memberId: Long, @Param("since") since: LocalDateTime): Long
}
