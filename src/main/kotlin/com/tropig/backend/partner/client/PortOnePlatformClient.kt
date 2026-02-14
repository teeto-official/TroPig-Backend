package com.tropig.backend.partner.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

// PortOne Platform API Request/Response DTOs
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

data class PlatformCreateManualTransferRequest(
    val partnerId: String,
    val settlementAmount: Long,
    val settlementTaxFreeAmount: Long? = null,
    val settlementDate: String,
    val memo: String? = null
)

data class PlatformCreateManualTransferResponse(
    val id: String,
    val partnerId: String,
    val settlementAmount: Long,
    val status: String,
    val createdAt: String?,
    val responseJson: String
)

// PortOne Partner API Request/Response DTOs (for PortOnePartnerClient)
data class CreatePartnerRequest(
    val id: String,
    val name: String,
    val contact: ContactInfo,
    val account: BankAccount,
    val type: PartnerType,
    val defaultContractId: String? = null
)

data class ContactInfo(
    val email: String
)

data class BankAccount(
    val bank: String,
    val accountNumber: String,
    val holder: String
)

enum class PartnerType {
    BUSINESS,
    NON_WHT_PAYER,
    WHT_PAYER
}

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
    val userDefinedProperties: Map<String, Any>
)

data class BankAccountHolderResponse(
    val accountHolder: String,
    val bank: String,
    val accountNumber: String,
    val verified: Boolean
)

class PortOnePlatformApiException(message: String) : RuntimeException(message)

@Component
class PortOnePlatformClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${portone.key.secret-v2}") private val apiSecret: String,
    @Value("\${portone.base-url:https://api.portone.io}") private val baseUrl: String,
) {
    
    /**
     * 계좌 예금주 조회
     * GET /platform/bank-accounts/{bank}/{accountNumber}/holder
     */
    fun getAccountHolder(
        bank: String,
        accountNumber: String,
        birthdate: String? = null,
        businessRegistrationNumber: String? = null,
    ): PlatformAccountHolderResponse {
        val uriBuilder = UriComponentsBuilder
            .fromHttpUrl("$baseUrl/platform/bank-accounts/$bank/$accountNumber/holder")

        birthdate?.let { uriBuilder.queryParam("birthdate", it) }
        businessRegistrationNumber?.let { uriBuilder.queryParam("businessRegistrationNumber", it) }

        val url = uriBuilder.toUriString()
        val headers = createHeaders()

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(null, headers),
            String::class.java
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw PortOnePlatformApiException("예금주 조회 실패: ${response.statusCode} ${response.body}")
        }

        val responseBody = objectMapper.readTree(response.body)
        return PlatformAccountHolderResponse(
            holderName = responseBody["holderName"]?.asText()
                ?: throw PortOnePlatformApiException("예금주명을 찾을 수 없습니다"),
            responseJson = response.body!!
        )
    }
    
    /**
     * 파트너 생성
     * POST /platform/partners
     */
    fun createPartner(request: PlatformCreatePartnerRequest): PlatformCreatePartnerResponse {
        val url = "$baseUrl/platform/partners"
        val headers = createHeaders()
        
        val typeMap = mutableMapOf<String, Any?>()
        request.type.individual?.let { individual ->
            val individualMap = mutableMapOf<String, Any?>()
            individual.residentRegistrationNumber?.let {
                individualMap["residentRegistrationNumber"] = it
            }
            if (individualMap.isNotEmpty()) {
                typeMap["individual"] = individualMap
            }
        }
        
        val body = mutableMapOf<String, Any?>(
            "id" to request.id,
            "name" to request.name,
            "contact" to mapOf(
                "email" to request.contact.email,
                "phoneNumber" to request.contact.phoneNumber
            ),
            "account" to mapOf(
                "bankCode" to request.account.bankCode,
                "accountNumber" to request.account.accountNumber,
                "accountHolderName" to request.account.accountHolderName
            ),
            "defaultContractId" to request.defaultContractId,
            "tags" to request.tags
        )
        
        if (typeMap.isNotEmpty()) {
            body["type"] = typeMap
        }
        
        val response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
        
        if (!response.statusCode.is2xxSuccessful) {
            throw PortOnePlatformApiException("파트너 생성 실패: ${response.statusCode} ${response.body}")
        }
        
        val responseBody = objectMapper.readTree(response.body)
        return PlatformCreatePartnerResponse(
            id = responseBody["id"]?.asText()
                ?: throw PortOnePlatformApiException("파트너 ID를 찾을 수 없습니다"),
            graphqlId = responseBody["graphqlId"]?.asText(),
            name = responseBody["name"]?.asText() ?: "",
            status = responseBody["status"]?.asText() ?: "UNKNOWN",
            createdAt = responseBody["createdAt"]?.asText(),
            responseJson = response.body!!
        )
    }
    
    /**
     * 수기 정산건 생성
     * POST /platform/transfers/manual
     */
    fun createManualTransfer(request: PlatformCreateManualTransferRequest): PlatformCreateManualTransferResponse {
        val url = "$baseUrl/platform/transfers/manual"
        val headers = createHeaders()
        
        val body = mutableMapOf<String, Any?>(
            "partnerId" to request.partnerId,
            "settlementAmount" to request.settlementAmount,
            "settlementDate" to request.settlementDate,
        )
        
        request.settlementTaxFreeAmount?.let {
            body["settlementTaxFreeAmount"] = it
        }
        
        request.memo?.let {
            body["memo"] = it
        }
        
        val response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
        
        if (!response.statusCode.is2xxSuccessful) {
            throw PortOnePlatformApiException("정산 생성 실패: ${response.statusCode} ${response.body}")
        }
        
        val responseBody = objectMapper.readTree(response.body)
        return PlatformCreateManualTransferResponse(
            id = responseBody["id"]?.asText()
                ?: throw PortOnePlatformApiException("정산 ID를 찾을 수 없습니다"),
            partnerId = responseBody["partnerId"]?.asText() ?: "",
            settlementAmount = responseBody["settlementAmount"]?.asLong() ?: 0L,
            status = responseBody["status"]?.asText() ?: "UNKNOWN",
            createdAt = responseBody["createdAt"]?.asText(),
            responseJson = response.body!!
        )
    }
    
    private fun createHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "PortOne $apiSecret")
        }
    }
}
