package com.tropig.backend.member.enums

/**
 * 통신사 목록
 * PortOne Identity Verification API에서 지원하는 통신사
 */
enum class Carrier {
    /** SK 텔레콤 */
    SKT,

    /** KT */
    KT,

    /** LG U+ */
    LGU,

    /** SK 텔레콤 알뜰폰 (MVNO) */
    SKT_MVNO,

    /** KT 알뜰폰 (MVNO) */
    KT_MVNO,

    /** LG U+ 알뜰폰 (MVNO) */
    LGU_MVNO,

    ;

    companion object {
        /**
         * 문자열로부터 Carrier enum을 생성합니다.
         * 대소문자 구분 없이 변환합니다.
         */
        fun fromString(value: String): Carrier = valueOf(value.uppercase())

        /**
         * 유효한 통신사 값인지 확인합니다.
         */
        fun isValid(value: String): Boolean = try {
            fromString(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
