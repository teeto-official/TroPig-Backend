package com.tropig.backend.recruitment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.enums.RecruitmentStatus
import com.tropig.backend.recruitment.repository.RecruitmentRepository
import org.springframework.stereotype.Component
import com.tropig.backend.common.exception.IllegalArgumentException as TroPigIllegalArgumentException

/** 구인글 조회/권한 검증 공통 로직 (삭제된 글 접근 차단, 작성자 확인) */
@Component
class RecruitmentAccessGuard(private val recruitmentRepository: RecruitmentRepository) {
    fun findVisible(id: Long): Recruitment = requireVisible(recruitmentRepository.findById(id).orElse(null), id)

    /** 상태 변경(수정/삭제/완료) 및 신청 시 동시 요청 경합을 막기 위해 행 잠금으로 조회한다. */
    fun findVisibleForUpdate(id: Long): Recruitment = requireVisible(recruitmentRepository.findByIdForUpdate(id), id)

    fun checkWriter(recruitment: Recruitment, memberId: Long) {
        if (recruitment.writerMemberId != memberId) {
            throw TroPigIllegalArgumentException("구인글 작성자만 가능합니다.", MessageCode.NOT_OWN_RECRUITMENT)
        }
    }

    private fun requireVisible(recruitment: Recruitment?, id: Long): Recruitment {
        if (recruitment == null) {
            throw NotFoundException("구인글을 찾을 수 없습니다. id: $id", MessageCode.NOT_FOUND_RECRUITMENT)
        }
        if (recruitment.deletedAt != null || recruitment.status == RecruitmentStatus.DELETED) {
            throw NotFoundException("삭제된 게시물입니다.", MessageCode.RECRUITMENT_DELETED)
        }

        return recruitment
    }
}
