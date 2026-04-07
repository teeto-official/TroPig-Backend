package com.tropig.backend.member.enums

/**
 * 한국 은행 코드 매핑
 */
enum class BankCode(val koreanName: String, val code: String) {
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
        fun fromKoreanName(koreanName: String): BankCode? = entries.find { it.koreanName == koreanName }

        fun getSupportedBankNames(): List<String> = entries.map { it.koreanName }
    }
}
