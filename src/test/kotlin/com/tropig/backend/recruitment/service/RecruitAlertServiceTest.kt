package com.tropig.backend.recruitment.service

import com.tropig.backend.recruitment.entity.RecruitAlert
import com.tropig.backend.recruitment.enums.RecruitActivityType
import com.tropig.backend.recruitment.repository.RecruitAlertRepository
import com.tropig.backend.recruitment.repository.RecruitmentApplicationRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecruitAlertServiceTest {

    private val recruitAlertRepository = mock(RecruitAlertRepository::class.java)
    private val applicationRepository = mock(RecruitmentApplicationRepository::class.java)

    private val service = RecruitAlertService(
        recruitAlertRepository = recruitAlertRepository,
        applicationRepository = applicationRepository,
    )

    private val epoch = LocalDateTime.of(1970, 1, 1, 0, 0)

    @Test
    fun `읽음 기록이 없으면 에포크 기준으로 미확인 여부를 계산한다`() {
        given(recruitAlertRepository.findById(1L)).willReturn(Optional.empty())
        given(applicationRepository.countUnreadHostingEvents(1L, epoch)).willReturn(2L)
        given(applicationRepository.countUnreadAppliedEvents(1L, epoch)).willReturn(0L)

        val response = service.getAlert(1L)

        assertEquals(true, response.hosting)
        assertEquals(false, response.applied)
    }

    @Test
    fun `읽음 기록 이후 이벤트가 없으면 알람이 꺼진다`() {
        val checkedAt = LocalDateTime.now().minusHours(1)
        val alert = RecruitAlert(memberId = 1L, lastCheckedHostingAt = checkedAt, lastCheckedAppliedAt = checkedAt)
        given(recruitAlertRepository.findById(1L)).willReturn(Optional.of(alert))
        given(applicationRepository.countUnreadHostingEvents(1L, checkedAt)).willReturn(0L)
        given(applicationRepository.countUnreadAppliedEvents(1L, checkedAt)).willReturn(0L)

        val response = service.getAlert(1L)

        assertEquals(false, response.hosting)
        assertEquals(false, response.applied)
    }

    @Test
    fun `읽음 처리하면 해당 탭의 확인 시각만 갱신된다`() {
        given(recruitAlertRepository.findById(1L)).willReturn(Optional.empty())
        given(recruitAlertRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer {
            it.arguments[0] as RecruitAlert
        }

        service.markRead(1L, RecruitActivityType.HOSTING)

        val captor = ArgumentCaptor.forClass(RecruitAlert::class.java)
        verify(recruitAlertRepository).save(captor.capture())
        assertNotNull(captor.value.lastCheckedHostingAt)
        assertNull(captor.value.lastCheckedAppliedAt)
    }
}
