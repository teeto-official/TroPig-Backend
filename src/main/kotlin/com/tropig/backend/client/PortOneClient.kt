package com.tropig.backend.client

import com.tropig.backend.common.exception.ExternalVerificationFailedException
import com.tropig.backend.common.exception.InvalidBankAccountException
import com.tropig.backend.common.model.PortOneErrorResponse
import com.tropig.backend.member.model.response.BankAccountHolderResponse
import com.tropig.backend.member.model.response.IdentityVerificationResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class PortOneClient(
    @Value("\${portone.base-url}") baseUrl: String,
    @Value("\${portone.secret-key}") private val secretKey: String,
) {
    private val client: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne $secretKey")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    suspend fun getBankAccountHolder(
        bank: String,
        accountNumber: String,
        birthdate: String?,
        businessRegistrationNumber: String? = null,
    ): BankAccountHolderResponse {
        return client.get()
            .uri { uriBuilder ->
                var b = uriBuilder
                    .path("/platform/bank-accounts/{bank}/{accountNumber}/holder")
                birthdate?.let {
                    b = b.queryParam("birthdate", it)
                }
                businessRegistrationNumber?.let {
                    b = b.queryParam("businessRegistrationNumber", it)
                }

                b.build(bank, accountNumber)
            }
            .retrieve()
            .onStatus({ it.is4xxClientError }) { response ->
                response.bodyToMono(PortOneErrorResponse::class.java)
                    .map { error ->
                        val errorMessage = when (error.type) {
                            "PLATFORM_NOT_ENABLED" -> "PortOne 플랫폼이 활성화되지 않았습니다. PortOne 관리자 페이지에서 계좌 인증 플랫폼을 활성화해주세요."
                            else -> error.message?.ifBlank { "계좌 인증에 실패했습니다" } ?: "계좌 인증에 실패했습니다 (에러 타입: ${error.type ?: "UNKNOWN"})"
                        }
                        InvalidBankAccountException(errorMessage)
                    }
            }
            .onStatus({ it.is5xxServerError }) { response ->
                response.bodyToMono(PortOneErrorResponse::class.java)
                    .map { error ->
                        ExternalVerificationFailedException(
                            "외부 계좌 인증 서비스 오류: ${error.message ?: "알 수 없는 오류"}"
                        )
                    }
            }
            .awaitBody()
    }

    suspend fun getIdentityVerification(identityVerificationId: String): IdentityVerificationResponse {
        return client.get()
            .uri("/identity-verifications/{id}", identityVerificationId)
            .retrieve()
            .awaitBody()
    }
}
