package com.tropig.backend.partner.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.IOException

/**
 * PortOne Partner API 클라이언트
 * 파트너 등록, 계좌 인증 등을 처리
 */
@Component
class PortOnePartnerClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${portone.key.secret-v2}") private val apiSecret: String,
    @Value("\${portone.base-url:https://api.portone.io}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(PortOnePartnerClient::class.java)

    /**
     * 파트너 생성
     * POST /platform/partners
     *
     * Retries on server errors (5xx) and network failures with exponential backoff:
     * - 1st retry: 1 second delay
     * - 2nd retry: 2 seconds delay
     * - 3rd retry: 4 seconds delay
     */
    @Retryable(
        retryFor = [HttpServerErrorException::class, IOException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    fun createPartner(request: CreatePartnerRequest): PartnerResponse {
        val url = "$baseUrl/platform/partners"
        val headers = createHeaders()

        logger.info("Creating PortOne partner: id=${request.id}")

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(request, headers),
                String::class.java
            )

            objectMapper.readValue(response.body, PartnerResponse::class.java)
        } catch (e: HttpClientErrorException) {
            logger.error("PortOne Partner API error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOnePartnerException("Failed to create partner: ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("PortOne Partner API server error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOnePartnerException("PortOne service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling PortOne API", e)
            throw PortOnePartnerException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * 파트너 계좌 정보 업데이트
     * PATCH /platform/partners/{partnerId}
     *
     * Retries on server errors (5xx) and network failures with exponential backoff.
     */
    @Retryable(
        retryFor = [HttpServerErrorException::class, IOException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    fun updatePartnerAccount(partnerId: String, account: BankAccount): PartnerResponse {
        val url = "$baseUrl/platform/partners/$partnerId"
        val headers = createHeaders()
        val body = mapOf("account" to account)

        logger.info("Updating PortOne partner account: partnerId=$partnerId")

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                HttpEntity(body, headers),
                String::class.java
            )

            objectMapper.readValue(response.body, PartnerResponse::class.java)
        } catch (e: HttpClientErrorException) {
            logger.error("PortOne Partner API error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOnePartnerException("Failed to update partner: ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("PortOne Partner API server error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOnePartnerException("PortOne service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling PortOne API", e)
            throw PortOnePartnerException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * 은행 계좌 보유자 조회 (계좌 실명 인증)
     * GET /platform/bank-accounts/{bank}/{accountNumber}/holder
     *
     * Retries on server errors (5xx) and network failures with exponential backoff.
     */
    @Retryable(
        retryFor = [HttpServerErrorException::class, IOException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    fun getBankAccountHolder(bankCode: String, accountNumber: String): String {
        val url = "$baseUrl/platform/bank-accounts/$bankCode/$accountNumber/holder"
        val headers = createHeaders()

        logger.info("Verifying bank account: bank=$bankCode, account=${maskAccountNumber(accountNumber)}")

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                String::class.java
            )

            val holderResponse = objectMapper.readValue(response.body, BankAccountHolderResponse::class.java)
            holderResponse.accountHolder
        } catch (e: HttpClientErrorException) {
            logger.error("PortOne Bank Account API error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOnePartnerException("Failed to verify bank account: ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("PortOne Bank Account API server error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOnePartnerException("PortOne service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling PortOne API", e)
            throw PortOnePartnerException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * HTTP 헤더 생성
     */
    private fun createHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "PortOne $apiSecret")
        }
    }

    /**
     * 계좌번호 마스킹 (로깅용)
     */
    private fun maskAccountNumber(accountNumber: String): String {
        return if (accountNumber.length >= 6) {
            "${accountNumber.substring(0, 3)}****${accountNumber.substring(accountNumber.length - 3)}"
        } else {
            "****"
        }
    }
}
