package com.tropig.backend.recruitment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.contents.repository.ContentOptionRepository
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.entity.RecruitmentApplication
import com.tropig.backend.recruitment.enums.PlayEnvironment
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.model.RecruitmentDetails
import com.tropig.backend.recruitment.model.RuleRef
import com.tropig.backend.recruitment.model.request.CompleteRecruitmentRequest
import com.tropig.backend.recruitment.model.request.CreateRecruitmentRequest
import com.tropig.backend.recruitment.model.request.RuleRefRequest
import com.tropig.backend.recruitment.model.response.RecruitMemberResponse
import com.tropig.backend.recruitment.model.response.RecruitmentDetailResponse
import com.tropig.backend.recruitment.repository.RecruitmentApplicationRepository
import com.tropig.backend.recruitment.repository.RecruitmentRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.tropig.backend.common.exception.IllegalArgumentException as TroPigIllegalArgumentException

class RecruitmentServiceTest {

    private val recruitmentRepository = mock(RecruitmentRepository::class.java)
    private val applicationRepository = mock(RecruitmentApplicationRepository::class.java)
    private val memberRepository = mock(MemberRepository::class.java)
    private val contentRepository = mock(ContentRepository::class.java)
    private val contentOptionRepository = mock(ContentOptionRepository::class.java)
    private val mapper = mock(RecruitmentMapper::class.java)

    private val service = RecruitmentService(
        recruitmentRepository = recruitmentRepository,
        applicationRepository = applicationRepository,
        memberRepository = memberRepository,
        contentRepository = contentRepository,
        contentOptionRepository = contentOptionRepository,
        accessGuard = RecruitmentAccessGuard(recruitmentRepository),
        mapper = mapper,
    )

    @Test
    fun `제목이 비어 있으면 등록 불가`() {
        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.createRecruitment(1L, request(title = " "))
        }

        assertEquals(MessageCode.INVALID_PARAMS, exception.code)
    }

    @Test
    fun `룰이 없으면 등록 불가`() {
        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.createRecruitment(1L, request(rules = emptyList()))
        }

        assertEquals(MessageCode.INVALID_PARAMS, exception.code)
    }

    @Test
    fun `플레이 환경이 없으면 등록 불가`() {
        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.createRecruitment(1L, request(environments = emptyList()))
        }

        assertEquals(MessageCode.INVALID_PARAMS, exception.code)
    }

    @Test
    fun `룰 참조에 선택과 직접 입력이 모두 없으면 등록 불가`() {
        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.createRecruitment(1L, request(rules = listOf(RuleRefRequest())))
        }

        assertEquals(MessageCode.INVALID_PARAMS, exception.code)
    }

    @Test
    fun `마감일시가 과거면 등록 불가`() {
        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.createRecruitment(1L, request(deadlineAt = LocalDateTime.now().minusMinutes(1)))
        }

        assertEquals(MessageCode.INVALID_PARAMS, exception.code)
    }

    @Test
    fun `정상 요청이면 저장하고 상세 응답을 반환`() {
        given(recruitmentRepository.save(any())).willAnswer { it.arguments[0] as Recruitment }
        given(mapper.toDetail(anyArg(), anyArg(), anyArg(), anyArg())).willReturn(dummyDetail())

        val response = service.createRecruitment(1L, request())

        assertNotNull(response)
    }

    @Test
    fun `작성자가 아니면 삭제 불가`() {
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment(writerMemberId = 1L))

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.deleteRecruitment(10L, 2L)
        }

        assertEquals(MessageCode.NOT_OWN_RECRUITMENT, exception.code)
    }

    @Test
    fun `삭제하면 상태가 삭제됨으로 바뀌고 deletedAt이 설정된다`() {
        val recruitment = recruitment(writerMemberId = 1L)
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment)
        given(recruitmentRepository.save(any())).willAnswer { it.arguments[0] as Recruitment }

        service.deleteRecruitment(10L, 1L)

        assertEquals(RecruitmentStatus.DELETED, recruitment.status)
        assertNotNull(recruitment.deletedAt)
    }

    @Test
    fun `삭제된 글은 조회 시 삭제됨 코드로 예외`() {
        val recruitment = recruitment(writerMemberId = 1L).apply {
            status = RecruitmentStatus.DELETED
            deletedAt = LocalDateTime.now()
        }
        given(recruitmentRepository.findById(10L)).willReturn(Optional.of(recruitment))

        val exception = assertThrows<NotFoundException> {
            service.getRecruitment(10L, null)
        }

        assertEquals(MessageCode.RECRUITMENT_DELETED, exception.code)
    }

    @Test
    fun `이미 모집완료된 글은 다시 완료 처리할 수 없다`() {
        val recruitment = recruitment(writerMemberId = 1L).apply {
            status = RecruitmentStatus.RECRUITMENT_COMPLETED
        }
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment)

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.completeRecruitment(10L, 1L, CompleteRecruitmentRequest(listOf(1L), "메시지"))
        }

        assertEquals(MessageCode.RECRUITMENT_ALREADY_COMPLETED, exception.code)
    }

    @Test
    fun `다른 구인글의 신청 건이 포함되면 완료 처리 불가`() {
        val recruitment = recruitment(writerMemberId = 1L)
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment)
        given(applicationRepository.findAllByIdInAndRecruitmentId(listOf(99L), 10L)).willReturn(emptyList())

        val exception = assertThrows<TroPigIllegalArgumentException> {
            service.completeRecruitment(10L, 1L, CompleteRecruitmentRequest(listOf(99L), "메시지"))
        }

        assertEquals(MessageCode.INVALID_APPLICATION, exception.code)
    }

    @Test
    fun `완료 처리하면 선정 신청이 선택되고 상태가 모집완료로 바뀐다`() {
        val recruitment = recruitment(writerMemberId = 1L)
        val application = RecruitmentApplication(recruitmentId = 10L, applicantMemberId = 2L, content = "신청합니다")
        given(recruitmentRepository.findByIdForUpdate(10L)).willReturn(recruitment)
        given(applicationRepository.findAllByIdInAndRecruitmentId(listOf(0L), 10L)).willReturn(listOf(application))
        given(recruitmentRepository.save(any())).willAnswer { it.arguments[0] as Recruitment }
        given(mapper.toDetail(anyArg(), anyArg(), anyArg(), anyArg())).willReturn(dummyDetail())

        service.completeRecruitment(10L, 1L, CompleteRecruitmentRequest(listOf(0L), "함께해요!"))

        assertTrue(application.selected)
        assertEquals(RecruitmentStatus.RECRUITMENT_COMPLETED, recruitment.status)
        assertEquals("함께해요!", recruitment.completionMessage)
        assertNotNull(recruitment.completedAt)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T {
        Mockito.any<T>()
        return null as T
    }

    private fun request(
        title: String = "구인합니다",
        rules: List<RuleRefRequest> = listOf(RuleRefRequest(text = "자작 룰")),
        environments: List<PlayEnvironment> = listOf(PlayEnvironment.ONLINE),
        deadlineAt: LocalDateTime = LocalDateTime.now().plusDays(7),
    ) = CreateRecruitmentRequest(
        title = title,
        rules = rules,
        environments = environments,
        deadlineAt = deadlineAt,
    )

    private fun recruitment(writerMemberId: Long) = Recruitment(
        writerMemberId = writerMemberId,
        title = "테스트 구인",
        status = RecruitmentStatus.RECRUITING,
        deadlineAt = LocalDateTime.now().plusDays(7),
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

    private fun dummyDetail() = RecruitmentDetailResponse(
        id = 10L,
        title = "테스트 구인",
        status = RecruitmentStatus.RECRUITING,
        deadlineAt = LocalDateTime.now().plusDays(7),
        environments = listOf(PlayEnvironment.ONLINE),
        scenarios = emptyList(),
        rules = emptyList(),
        gm = emptyList(),
        pl = emptyList(),
        schedules = emptyList(),
        playTimeHours = null,
        playTimeText = null,
        overview = null,
        caution = null,
        notice = null,
        writer = RecruitMemberResponse(memberId = 1L, nickname = "작성자"),
        applicationCount = 0L,
        isWriter = true,
        myApplication = null,
        completionMessage = null,
        completedAt = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )
}
