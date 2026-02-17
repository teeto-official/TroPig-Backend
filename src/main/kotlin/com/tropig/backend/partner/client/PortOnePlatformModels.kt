package com.tropig.backend.partner.client

// PortOne Platform API Request DTOs

data class PlatformCreatePartnerRequest(
    val id: String,
    val name: String,
    val contact: PlatformContactInfo,
    val account: PlatformAccountInfo,
    val defaultContractId: String,
    val type: PlatformPartnerType,
    val tags: List<String> = emptyList()
)

data class PlatformContactInfo(
    val email: String,
    val phoneNumber: String
)

data class PlatformAccountInfo(
    val bankCode: String,
    val accountNumber: String,
    val accountHolderName: String
)

data class PlatformPartnerType(
    val individual: PlatformIndividualType? = null
)

data class PlatformIndividualType(
    val residentRegistrationNumber: String? = null
)

data class PlatformCreateManualTransferRequest(
    val partnerId: String,
    val settlementAmount: Long,
    val settlementTaxFreeAmount: Long? = null,
    val settlementDate: String,
    val memo: String? = null
)

// PortOne Platform API Response DTOs

data class PlatformAccountHolderResponse(
    val holderName: String,
    val responseJson: String
)

data class PlatformCreatePartnerResponse(
    val id: String,
    val graphqlId: String?,
    val name: String,
    val status: String,
    val createdAt: String?,
    val responseJson: String
)

data class PlatformCreateManualTransferResponse(
    val id: String,
    val partnerId: String,
    val settlementAmount: Long,
    val status: String,
    val createdAt: String?,
    val responseJson: String
)

// PortOne Platform API Exception

class PortOnePlatformApiException(message: String) : RuntimeException(message)
