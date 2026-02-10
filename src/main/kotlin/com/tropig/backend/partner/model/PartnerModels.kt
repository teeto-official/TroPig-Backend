package com.tropig.backend.partner.model

/**
 * PortOne Partner API 모델
 */

/**
 * 파트너 생성 요청
 */
data class CreatePartnerRequest(
    val id: String,  // TroPig member ID
    val name: String,
    val contact: ContactInfo,
    val account: BankAccount,
    val type: PartnerType,
    val defaultContractId: String? = null
)

/**
 * 연락처 정보
 */
data class ContactInfo(
    val email: String
)

/**
 * 은행 계좌 정보
 */
data class BankAccount(
    val bank: String,  // PortOne bank code (e.g., "SHINHAN")
    val accountNumber: String,
    val holder: String
)

/**
 * 파트너 유형
 */
enum class PartnerType {
    BUSINESS,           // 법인 사업자
    NON_WHT_PAYER,     // 원천징수 미대상자 (개인)
    WHT_PAYER          // 원천징수 대상자
}

/**
 * 파트너 응답
 */
data class PartnerResponse(
    val id: String,  // PortOne partner ID
    val graphqlId: String,
    val name: String,
    val contact: ContactInfo,
    val account: BankAccount,
    val type: PartnerType,
    val defaultContractId: String?,
    val memo: String?,
    val tags: List<String>,
    val userDefinedProperties: Map<String, Any>
)

/**
 * 은행 계좌 보유자 응답
 */
data class BankAccountHolderResponse(
    val accountHolder: String,
    val bank: String,
    val accountNumber: String,
    val verified: Boolean
)
