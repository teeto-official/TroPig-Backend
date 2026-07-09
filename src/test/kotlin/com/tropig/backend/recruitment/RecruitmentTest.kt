package com.tropig.backend.recruitment

import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.enums.PlayEnvironment
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.model.RecruitmentDetails
import com.tropig.backend.recruitment.model.RuleRef
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class RecruitmentTest {

    @Test
    fun `모집중 + 마감 전이면 모집중 상태`() {
        val now = LocalDateTime.now()
        val recruitment = recruitment(status = RecruitmentStatus.RECRUITING, deadlineAt = now.plusHours(1))

        assertEquals(RecruitmentStatus.RECRUITING, recruitment.effectiveStatus(now))
    }

    @Test
    fun `모집중 + 마감 경과면 마감 상태로 파생`() {
        val now = LocalDateTime.now()
        val recruitment = recruitment(status = RecruitmentStatus.RECRUITING, deadlineAt = now.minusMinutes(1))

        assertEquals(RecruitmentStatus.CLOSED, recruitment.effectiveStatus(now))
    }

    @Test
    fun `모집완료 상태는 마감 경과와 무관하게 유지`() {
        val now = LocalDateTime.now()
        val recruitment = recruitment(status = RecruitmentStatus.RECRUITMENT_COMPLETED, deadlineAt = now.minusDays(1))

        assertEquals(RecruitmentStatus.RECRUITMENT_COMPLETED, recruitment.effectiveStatus(now))
    }

    @Test
    fun `삭제 상태는 그대로 유지`() {
        val now = LocalDateTime.now()
        val recruitment = recruitment(status = RecruitmentStatus.DELETED, deadlineAt = now.minusDays(1))

        assertEquals(RecruitmentStatus.DELETED, recruitment.effectiveStatus(now))
    }

    private fun recruitment(status: RecruitmentStatus, deadlineAt: LocalDateTime) = Recruitment(
        writerMemberId = 1L,
        title = "테스트 구인",
        status = status,
        deadlineAt = deadlineAt,
        playTimeHours = null,
        playTimeText = null,
        overview = null,
        caution = null,
        notice = null,
        details = RecruitmentDetails(
            rules = listOf(RuleRef(text = "자유 룰")),
            environments = listOf(PlayEnvironment.ONLINE),
        ),
    )
}
