package com.tropig.backend.member.model.response

data class AccountAuthResponse(
    val holderName: String,
    val bankName: String,
    val bankAccount: String
)

data class AdultAuthResponse(
    val isAdult: Boolean
)