package com.tropig.backend.member.enums

/**
 * 한국 은행 코드 매핑
 * 한글 은행명을 PortOne 은행 코드로 변환
 */
enum class BankCode(val koreanName: String, val portOneCode: String) {
    SHINHAN("신한은행", "SHINHAN"),
    KOOKMIN("국민은행", "KOOKMIN"),
    WOORI("우리은행", "WOORI"),
    HANA("하나은행", "HANA"),
    NH("농협은행", "NH"),
    IBK("기업은행", "IBK"),
    BUSAN("부산은행", "BUSAN"),
    DAEGU("대구은행", "DAEGU"),
    JEONBUK("전북은행", "JEONBUK"),
    GYEONGNAM("경남은행", "GYEONGNAM"),
    KWANGJU("광주은행", "KWANGJU"),
    SC("SC제일은행", "SC"),
    CITY("한국씨티은행", "CITY"),
    KAKAO("카카오뱅크", "KAKAO"),
    KBANK("케이뱅크", "KBANK"),
    TOSS("토스뱅크", "TOSS"),
    ;

    companion object {
        /**
         * 한글 은행명으로 BankCode 찾기
         */
        fun fromKoreanName(koreanName: String): BankCode? = values().find { it.koreanName == koreanName }

        /**
         * 한글 은행명을 PortOne 은행 코드로 변환
         * @throws IllegalArgumentException 지원하지 않는 은행인 경우
         */
        fun toPortOneCode(koreanName: String): String = fromKoreanName(koreanName)?.portOneCode
            ?: throw IllegalArgumentException("Unsupported bank: $koreanName")

        /**
         * 지원되는 모든 한글 은행명 목록
         */
        fun getSupportedBankNames(): List<String> = values().map { it.koreanName }
    }
}
