package com.tropig.backend.member.model.response

data class BankAccountHolderResponse(
    val holder: String, // 실제 스키마는 문서/응답에 맞춰 조정
)

data class IdentityVerificationResponse(
    val status: String,
    val verifiedCustomer: VerifiedCustomer,
) {
    data class VerifiedCustomer(
        val ci: String? = null,
        val di: String? = null,
        val name: String? = null,
        val birthDate: String? = null,   // YYYY-MM-DD :contentReference[oaicite:9]{index=9}
        val phoneNumber: String? = null,
        val gender: String? = null,
        val isForeigner: Boolean? = null,
    )
}