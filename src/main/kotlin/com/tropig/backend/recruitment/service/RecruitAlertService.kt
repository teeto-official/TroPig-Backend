package com.tropig.backend.recruitment.service

import com.tropig.backend.recruitment.entity.RecruitAlert
import com.tropig.backend.recruitment.enums.RecruitActivityType
import com.tropig.backend.recruitment.model.response.RecruitAlertResponse
import com.tropig.backend.recruitment.repository.RecruitAlertRepository
import com.tropig.backend.recruitment.repository.RecruitmentApplicationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RecruitAlertService(
    private val recruitAlertRepository: RecruitAlertRepository,
    private val applicationRepository: RecruitmentApplicationRepository,
) {
    companion object {
        private val EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0)
    }

    @Transactional(readOnly = true)
    fun getAlert(memberId: Long): RecruitAlertResponse {
        val alert = recruitAlertRepository.findById(memberId).orElse(null)
        val hosting = applicationRepository.countUnreadHostingEvents(
            memberId,
            alert?.lastCheckedHostingAt ?: EPOCH,
        ) > 0
        val applied = applicationRepository.countUnreadAppliedEvents(
            memberId,
            alert?.lastCheckedAppliedAt ?: EPOCH,
        ) > 0

        return RecruitAlertResponse(hosting = hosting, applied = applied)
    }

    @Transactional
    fun markRead(memberId: Long, type: RecruitActivityType) {
        val now = LocalDateTime.now()
        val alert = recruitAlertRepository.findById(memberId).orElseGet { RecruitAlert(memberId) }

        when (type) {
            RecruitActivityType.HOSTING -> alert.lastCheckedHostingAt = now
            RecruitActivityType.APPLIED -> alert.lastCheckedAppliedAt = now
        }
        alert.updatedAt = now
        recruitAlertRepository.save(alert)
    }
}
