package com.tropig.backend.recruitment.repository

import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RecruitmentRepository :
    JpaRepository<Recruitment, Long>,
    RecruitmentCustomRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recruitment r WHERE r.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): Recruitment?

    fun findAllByWriterMemberIdAndDeletedAtIsNull(writerMemberId: Long, pageable: Pageable): Page<Recruitment>

    @Modifying
    @Query(
        """
        UPDATE Recruitment r
        SET r.status = :toStatus, r.updatedAt = :now
        WHERE r.status = :fromStatus AND r.deadlineAt < :now AND r.deletedAt IS NULL
        """,
    )
    fun closeExpiredRecruitments(
        @Param("fromStatus") fromStatus: RecruitmentStatus,
        @Param("toStatus") toStatus: RecruitmentStatus,
        @Param("now") now: LocalDateTime,
    ): Int
}
