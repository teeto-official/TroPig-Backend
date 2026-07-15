package com.tropig.backend.recruitment

import com.tropig.backend.recruitment.enums.RecruitmentBadge
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RecruitmentBadgeTest {

    @Test
    fun `모집중 상태는 선정 여부와 무관하게 모집중 배지`() {
        assertEquals(RecruitmentBadge.RECRUITING, RecruitmentBadge.of(RecruitmentStatus.RECRUITING, false))
        assertEquals(RecruitmentBadge.RECRUITING, RecruitmentBadge.of(RecruitmentStatus.RECRUITING, true))
    }

    @Test
    fun `모집완료 + 선정됨은 모집됨 배지`() {
        assertEquals(RecruitmentBadge.SELECTED, RecruitmentBadge.of(RecruitmentStatus.RECRUITMENT_COMPLETED, true))
    }

    @Test
    fun `모집완료 + 선정 안 됨은 모집완료 배지`() {
        assertEquals(RecruitmentBadge.COMPLETED, RecruitmentBadge.of(RecruitmentStatus.RECRUITMENT_COMPLETED, false))
    }

    @Test
    fun `마감 상태는 마감 배지`() {
        assertEquals(RecruitmentBadge.CLOSED, RecruitmentBadge.of(RecruitmentStatus.CLOSED, false))
        assertEquals(RecruitmentBadge.CLOSED, RecruitmentBadge.of(RecruitmentStatus.CLOSED, true))
    }

    @Test
    fun `삭제 상태는 삭제됨 배지`() {
        assertEquals(RecruitmentBadge.DELETED, RecruitmentBadge.of(RecruitmentStatus.DELETED, true))
    }

    @Test
    fun `진행중 상태는 선정 여부에 따라 모집됨 또는 모집완료 배지`() {
        assertEquals(RecruitmentBadge.SELECTED, RecruitmentBadge.of(RecruitmentStatus.IN_PROGRESS, true))
        assertEquals(RecruitmentBadge.COMPLETED, RecruitmentBadge.of(RecruitmentStatus.IN_PROGRESS, false))
    }
}
