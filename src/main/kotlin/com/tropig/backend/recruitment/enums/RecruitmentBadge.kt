package com.tropig.backend.recruitment.enums

enum class RecruitmentBadge {
    RECRUITING,
    SELECTED,
    COMPLETED,
    CLOSED,
    DELETED,
    ;

    companion object {
        fun of(status: RecruitmentStatus, selected: Boolean): RecruitmentBadge = when (status) {
            RecruitmentStatus.RECRUITING -> RECRUITING
            RecruitmentStatus.RECRUITMENT_COMPLETED, RecruitmentStatus.IN_PROGRESS ->
                if (selected) SELECTED else COMPLETED
            RecruitmentStatus.CLOSED -> CLOSED
            RecruitmentStatus.DELETED -> DELETED
        }
    }
}
