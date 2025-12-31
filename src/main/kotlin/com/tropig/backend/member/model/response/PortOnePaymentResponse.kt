package com.tropig.backend.member.model.response

data class PortOnePaymentResponse(
    val status: String,
    val method: PaymentMethod
) {
    data class PaymentMethod(
        val type: String,
        val bank: Bank?
    )

    data class Bank(
        val name: String,
        val accountNumber: String
    )
}