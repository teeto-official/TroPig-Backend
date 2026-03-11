package com.tropig.backend.partner.client

// PortOne Partner API Request DTOs

data class CreatePartnerRequest(
    val id: String,
    val name: String,
    val contact: ContactInfo,
    val account: BankAccount,
    val type: PartnerType,
    val defaultContractId: String? = null,
)

data class ContactInfo(val email: String)

data class BankAccount(val bank: String, val accountNumber: String, val holder: String)

enum class PartnerType {
    BUSINESS,
    NON_WHT_PAYER,
    WHT_PAYER,
}

// PortOne Partner API Response DTOs

data class PartnerResponse(
    val id: String,
    val graphqlId: String,
    val name: String,
    val contact: ContactInfo,
    val account: BankAccount,
    val type: PartnerType,
    val defaultContractId: String?,
    val memo: String?,
    val tags: List<String>,
    val userDefinedProperties: Map<String, Any>,
)

data class BankAccountHolderResponse(
    val accountHolder: String,
    val bank: String,
    val accountNumber: String,
    val verified: Boolean,
)

// PortOne Partner API Exception

class PortOnePartnerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
