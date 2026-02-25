package com.tropig.backend.member.enums

/**
 * 본인인증 방법
 */
enum class VerificationMethod {
    /** SMS 인증 (OTP 코드) */
    SMS,

    /** 앱 인증 (통신사 앱) */
    APP,

    ;

    companion object {
        fun fromString(value: String): VerificationMethod = valueOf(value.uppercase())

        fun isValid(value: String): Boolean = try {
            fromString(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
