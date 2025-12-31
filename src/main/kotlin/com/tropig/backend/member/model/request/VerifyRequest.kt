package com.tropig.backend.member.model.request

data class AccountAuthRequest(
    val bank: String,
    val accountNumber: String,
    val birthdate: String? = null,
    val businessRegistrationNumber: String? = null,
    val test: Boolean? = null,
)

data class AdultAuthRequest(
    val identityVerificationId: String
)