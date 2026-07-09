package com.tropig.backend.recruitment.enums

enum class RecruitmentStatus {
    RECRUITING,
    RECRUITMENT_COMPLETED,
    CLOSED,

    /** 세션 진행중 상태. 전환 로직은 이번 버전 범위에서 제외되어 아직 할당되지 않는다. */
    IN_PROGRESS,
    DELETED,
}
