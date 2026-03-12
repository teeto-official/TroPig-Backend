package com.tropig.backend.member.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

/**
 * PortOne Identity Verification API 클라이언트
 * 본인인증 서비스 연동
 */
@Component
class PortOneIdentityVerificationClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${portone.key.secret-v2}") private val apiSecret: String,
    @Value("\${portone.identity-verification.channel-key}") private val channelKey: String,
    @Value("\${portone.base-url:https://api.portone.io}") private val baseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(PortOneIdentityVerificationClient::class.java)

    /**
     * 본인인증 요청 전송 (SMS/APP)
     * POST /identity-verifications/{identityVerificationId}/send
     */
    fun sendVerification(request: SendVerificationRequest): SendVerificationResponse {
        val url = "$baseUrl/identity-verifications/${request.identityVerificationId}/send"
        val headers = createHeaders()

        val body = mapOf(
            "channelKey" to channelKey,
            "customer" to mapOf(
                "name" to request.customer.name,
                "phoneNumber" to request.customer.phoneNumber,
                "ipAddress" to request.customer.ipAddress,
                "identityNumber" to request.customer.identityNumber,
            ),
            "operator" to request.operator,
            "method" to request.method,
            "customData" to request.customData,
        )

        logger.info("Sending identity verification request: identityVerificationId=${request.identityVerificationId}")
        logger.debug("PortOne API URL: $url")
        logger.debug("Channel Key: ${channelKey.take(10)}...")
        logger.debug(
            "API Secret configured: ${if (apiSecret.isNotBlank()) {
                "Yes (${apiSecret.take(
                    10,
                )}...)"
            } else {
                "NO - MISSING!"
            }}",
        )

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(body, headers),
                String::class.java,
            )

            val jsonNode = objectMapper.readTree(response.body)
            SendVerificationResponse(
                id = jsonNode.get("id").asText(),
                status = jsonNode.get("status").asText(),
                requestedAt = LocalDateTime.parse(jsonNode.get("requestedAt").asText()),
            )
        } catch (e: HttpClientErrorException) {
            logger.error("PortOne API client error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOneIdentityVerificationException("Failed to send verification: ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("PortOne API server error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOneIdentityVerificationException("PortOne service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling PortOne API", e)
            throw PortOneIdentityVerificationException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * 본인인증 확인 (OTP 제출)
     * POST /identity-verifications/{identityVerificationId}/confirm
     */
    fun confirmVerification(identityVerificationId: String, otp: String): ConfirmVerificationResponse {
        val url = "$baseUrl/identity-verifications/$identityVerificationId/confirm"
        val headers = createHeaders()

        val body = mapOf("otp" to otp)

        logger.info("Confirming identity verification: identityVerificationId=$identityVerificationId")

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(body, headers),
                String::class.java,
            )

            val jsonNode = objectMapper.readTree(response.body)
            val verifiedCustomer = jsonNode.get("verifiedCustomer")

            ConfirmVerificationResponse(
                id = jsonNode.get("id").asText(),
                status = jsonNode.get("status").asText(),
                verifiedCustomer = parseVerifiedCustomer(verifiedCustomer),
                verifiedAt = LocalDateTime.parse(jsonNode.get("verifiedAt").asText()),
            )
        } catch (e: HttpClientErrorException) {
            val errorMessage = e.responseBodyAsString
            logger.error("PortOne API client error: ${e.statusCode} - $errorMessage")

            // Parse error code from PortOne response
            val errorCode = try {
                val errorNode = objectMapper.readTree(errorMessage)
                errorNode.get("type")?.asText() ?: "UNKNOWN"
            } catch (ex: Exception) {
                "UNKNOWN"
            }

            throw PortOneIdentityVerificationException(
                message = "Failed to confirm verification: $errorCode",
                cause = e,
                errorCode = errorCode,
            )
        } catch (e: HttpServerErrorException) {
            logger.error("PortOne API server error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOneIdentityVerificationException("PortOne service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling PortOne API", e)
            throw PortOneIdentityVerificationException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * 본인인증 결과 조회 (SDK 방식)
     * GET /identity-verifications/{identityVerificationId}
     */
    fun getVerification(identityVerificationId: String): ConfirmVerificationResponse {
        val url = "$baseUrl/identity-verifications/$identityVerificationId"
        val headers = createHeaders()

        logger.info("Getting identity verification result: identityVerificationId=$identityVerificationId")

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                String::class.java,
            )

            val jsonNode = objectMapper.readTree(response.body)
            val status = jsonNode.get("status").asText()

            if (status != "VERIFIED") {
                throw PortOneIdentityVerificationException(
                    message = "본인인증이 완료되지 않았습니다. 현재 상태: $status",
                    errorCode = "IDENTITY_VERIFICATION_NOT_VERIFIED",
                )
            }

            val verifiedCustomer = jsonNode.get("verifiedCustomer")
            ConfirmVerificationResponse(
                id = jsonNode.get("id").asText(),
                status = status,
                verifiedCustomer = parseVerifiedCustomer(verifiedCustomer),
                verifiedAt = LocalDateTime.parse(jsonNode.get("verifiedAt").asText()),
            )
        } catch (e: PortOneIdentityVerificationException) {
            throw e
        } catch (e: HttpClientErrorException) {
            logger.error("PortOne API client error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOneIdentityVerificationException("Failed to get verification: ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("PortOne API server error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOneIdentityVerificationException("PortOne service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling PortOne API", e)
            throw PortOneIdentityVerificationException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * OTP 재전송
     * POST /identity-verifications/{identityVerificationId}/resend
     */
    fun resendVerification(identityVerificationId: String): ResendVerificationResponse {
        val url = "$baseUrl/identity-verifications/$identityVerificationId/resend"
        val headers = createHeaders()

        logger.info("Resending OTP: identityVerificationId=$identityVerificationId")

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                String::class.java,
            )

            val jsonNode = objectMapper.readTree(response.body)
            ResendVerificationResponse(
                id = jsonNode.get("id").asText(),
                status = jsonNode.get("status").asText(),
            )
        } catch (e: HttpClientErrorException) {
            logger.error("PortOne API client error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOneIdentityVerificationException("Failed to resend OTP: ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("PortOne API server error: ${e.statusCode} - ${e.responseBodyAsString}")
            throw PortOneIdentityVerificationException("PortOne service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling PortOne API", e)
            throw PortOneIdentityVerificationException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * PortOne 응답에서 VerifiedCustomer를 파싱합니다.
     * ci, di, operator는 nullable하게 처리합니다.
     */
    private fun parseVerifiedCustomer(node: com.fasterxml.jackson.databind.JsonNode): VerifiedCustomer =
        VerifiedCustomer(
            name = node.get("name").asText(),
            phoneNumber = node.get("phoneNumber").asText(),
            birthDate = node.get("birthDate").asText(),
            gender = node.get("gender").asText(),
            isForeigner = node.get("isForeigner").asBoolean(),
            ci = node.get("ci").asText(),
            di = node.get("di").asText(),
            operator = node.get("operator").asText(),
        )

    /**
     * HTTP 헤더 생성
     */
    private fun createHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "PortOne $apiSecret")
        return headers
    }
}

/**
 * 본인인증 요청 데이터
 */
data class SendVerificationRequest(
    val identityVerificationId: String,
    val customer: CustomerInfo,
    val operator: String,
    val method: String,
    val customData: String? = null,
)

data class CustomerInfo(val name: String, val phoneNumber: String, val ipAddress: String, val identityNumber: String)

/**
 * 본인인증 요청 응답
 */
data class SendVerificationResponse(val id: String, val status: String, val requestedAt: LocalDateTime)

/**
 * 본인인증 확인 응답
 */
data class ConfirmVerificationResponse(
    val id: String,
    val status: String,
    val verifiedCustomer: VerifiedCustomer,
    val verifiedAt: LocalDateTime,
)

data class VerifiedCustomer(
    val name: String,
    val phoneNumber: String,
    val birthDate: String, // YYYY-MM-DD
    val gender: String,
    val isForeigner: Boolean,
    val ci: String,
    val di: String,
    val operator: String,
)

/**
 * OTP 재전송 응답
 */
data class ResendVerificationResponse(val id: String, val status: String)

/**
 * PortOne Identity Verification API 예외
 */
class PortOneIdentityVerificationException(message: String, cause: Throwable? = null, val errorCode: String? = null) :
    RuntimeException(message, cause)
