package com.tropig.backend.common.enums

enum class Genre {
    GAME_FANTASY, // 게임판타지
    GORE, // 고어
    HORROR, // 공포
    MILITARY, // 군부
    NOIR, // 느와르
    DARK_FANTASY, // 다크판타지
    FAIRY_TALE, // 동화
    DYSTOPIA, // 디스토피아
    DRAMA, // 드라마
    ROMCOM, // 러브코미디
    ROMANCE, // 로맨스
    ROMANCE_FANTASY, // 로맨스 판타지
    MELANCHOLY, // 멜랑콜리
    ADVENTURE, // 모험
    WUXIA, // 무협
    MYSTERY, // 미스터리
    CRIME, // 범죄
    BLACK_COMEDY, // 블랙코미디
    CYBERPUNK, // 사이버펑크
    WESTERN, // 서부극
    SUSPENSE, // 서스펜스
    INVESTIGATION, // 수사
    PURE_ROMANCE, // 순정
    THRILLER, // 스릴러
    SPORTS, // 스포츠
    HISTORICAL, // 시대극
    APOCALYPSE, // 아포칼립스
    ACTION, // 액션
    TRAVEL, // 여행
    HISTORY, // 역사
    SUPERNATURAL, // 이능력
    ZOMBIE, // 좀비
    MEDIEVAL, // 중세시대
    YOUTH, // 청춘
    DETECTIVE, // 추리
    CAMPUS, // 캠퍼스
    COMEDY, // 코미디
    FANTASY, // 판타지
    POST_APOCALYPSE, // 포스트아포칼립스
    SCHOOL, // 학교
    URBAN_FANTASY, // 현대판타지
    HERO_VILLAIN, // 히어로빌런
    HEALING, // 힐링
    SF, // SF
    ;

    companion object {
        fun fromList(genres: String?): List<Genre> {
            return genres?.split(",")
                ?.map { Genre.valueOf(it) }
                ?: emptyList()
        }
    }
}