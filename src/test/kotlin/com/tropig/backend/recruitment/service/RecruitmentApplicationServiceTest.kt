package com.tropig.backend.recruitment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.enums.PlayEnvironment
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.model.RecruitmentDetails
import com.tropig.backend.recruitment.model.RuleRef
import com.tropig.backend.recruitment.model.request.CreateApplicationRequest
import com.tropig.backend.recruitment.repository.RecruitmentApplicationRepository
import com.tropig.backend.recruitment.repository.RecruitmentRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import com.tropig.backend.common.exception.IllegalArgumentException as TroPigIllegalArgumentException

class RecruitmentApplicationServiceTest {

    private val recruitmentRepository = mock(RecruitmentRepository::class.java)
    private val applicationRepository = mock(RecruitmentApplicationRepository::class.java)
    private val mapper = mock(RecruitmentMapper::class.java)

    private val service = RecruitmentApplicationService(
        recruitmentRepository = recruitmentRepository,
        applicationRepository = applicationRepository,
        accessGuard = RecruitmentAccessGuard(recruitmentRepository),
        mapper = mapper,
    )

    @Test
    fun `본인이 작성한 구인글에는 신청할 수 없다`() {
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment(writerMemberId = 1L))

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.apply(10L, 1L, CreateApplicationRequest("신청합니다"))
        }

        assertEquals(MessageCode.CANNOT_APPLY_OWN_RECRUITMENT, exception.code)
    }

    @Test
    fun `마감일이 지난 구인글에는 신청할 수 없다`() {
        val recruitment = recruitment(writerMemberId = 1L, deadlineAt = LocalDateTime.now().minusMinutes(1))
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment)

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.apply(10L, 2L, CreateApplicationRequest("신청합니다"))
        }

        assertEquals(MessageCode.NOT_RECRUITING, exception.code)
    }

    @Test
    fun `모집완료된 구인글에는 신청할 수 없다`() {
        val recruitment = recruitment(writerMemberId = 1L).apply {
            status = RecruitmentStatus.RECRUITMENT_COMPLETED
        }
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment)

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.apply(10L, 2L, CreateApplicationRequest("신청합니다"))
        }

        assertEquals(MessageCode.NOT_RECRUITING, exception.code)
    }

    @Test
    fun `신청 내용이 비어 있으면 신청 불가`() {
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment(writerMemberId = 1L))

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.apply(10L, 2L, CreateApplicationRequest("  "))
        }

        assertEquals(MessageCode.INVALID_PARAMS, exception.code)
    }

    @Test
    fun `이미 신청한 구인글에는 다시 신청할 수 없다`() {
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment(writerMemberId = 1L))
        given(applicationRepository.existsByRecruitmentIdAndApplicantMemberId(10L, 2L)).willReturn(true)

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.apply(10L, 2L, CreateApplicationRequest("신청합니다"))
        }

        assertEquals(MessageCode.ALREADY_APPLIED, exception.code)
    }

    @Test
    fun `작성자가 아니면 신청자 목록을 볼 수 없다`() {
        given(recruitmentRepository.findById(10L)).willReturn(Optional.of(recruitment(writerMemberId = 1L)))

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.getApplications(10L, 2L)
        }

        assertEquals(MessageCode.NOT_OWN_RECRUITMENT, exception.code)
    }

    private fun recruitment(writerMemberId: Long, deadlineAt: LocalDateTime = LocalDateTime.now().plusDays(7)) =
        Recruitment(
            writerMemberId = writerMemberId,
            title = "테스트 구인",
            status = RecruitmentStatus.RECRUITING,
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
