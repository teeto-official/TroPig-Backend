package com.tropig.backend.contents.enums

enum class ContentsStatus {
    PRIVATE, // 비공개
    DRAFT, // 임시 저장
    PUBLISHED, // 공개(발행완료)
    DELETED, // 삭제
    BANNED, // 관리자 비공개
    ;

    companion object {
        val purchasedStatuses: List<ContentsStatus> = listOf(PUBLISHED, PRIVATE)
        val authorStatuses: List<ContentsStatus> = listOf(DRAFT, PRIVATE, PUBLISHED, BANNED)
        val publicStatuses: List<ContentsStatus> = listOf(PUBLISHED)
    }
}
