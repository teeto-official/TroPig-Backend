package com.tropig.backend.contents.enums

enum class ContentsStatus {
    PRIVATE,   // 비활성화
    DRAFT,  // 임시 저장
    PUBLISHED,  // 발행완료
    DELETED,    // 삭제
    ;

    companion object {
        val purchasedStatuses: List<ContentsStatus> = listOf(PUBLISHED, PRIVATE)
        val authorStatuses: List<ContentsStatus> = listOf(DRAFT, PRIVATE, PUBLISHED)
        val publicStatuses: List<ContentsStatus> = listOf(PUBLISHED)
    }
}