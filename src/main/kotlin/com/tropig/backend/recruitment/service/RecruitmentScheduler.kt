package com.tropig.backend.recruitment.service

import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.repository.RecruitmentRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class RecruitmentScheduler(private val recruitmentRepository: RecruitmentRepository) {
    companion object {
        private val logger = LoggerFactory.getLogger(RecruitmentScheduler::class.java)
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    fun closeExpiredRecruitments() {
        val closedCount = recruitmentRepository.closeExpiredRecruitments(
            fromStatus = RecruitmentStatus.RECRUITING,
            toStatus = RecruitmentStatus.CLOSED,
            now = LocalDateTime.now(),
        )

        if (closedCount > 0) {
            logger.info("마감일이 지난 구인글 $closedCount 건을 마감 처리했습니다.")
        }
    }
}
