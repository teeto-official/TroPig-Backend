package com.tropig.backend.member.model.response

data class PortOneIdentityResponse(
    val status: String,
    val verifiedCustomer: VerifiedCustomer
) {
    data class VerifiedCustomer(
        val birthDate: String // yyyy-MM-dd
    )
}