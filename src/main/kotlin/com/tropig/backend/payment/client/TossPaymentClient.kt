package com.tropig.backend.payment.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.Base64

@Component
class TossPaymentClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${toss.secret-key}") private val secretKey: String,
    @Value("\${toss.base-url:https://api.tosspayments.com}") private val baseUrl: String,
) {

    /**
     * 결제 승인 (POST /v1/payments/confirm)
     * 프론트엔드에서 결제 완료 후 서버에서 최종 승인 처리
     */
    fun confirmPayment(paymentKey: String, orderId: String, amount: Long): TossPaymentResponse {
        val url = "$baseUrl/v1/payments/confirm"
        val headers = createHeaders()

        val body = mapOf(
            "paymentKey" to paymentKey,
            "orderId" to orderId,
            "amount" to amount,
        )

        val response = try {
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(body, headers),
                String::class.java,
            )
        } catch (e: HttpClientErrorException) {
            val errorBody = e.responseBodyAsString
            val errorNode = objectMapper.readTree(errorBody)
            val code = errorNode["code"]?.asText() ?: "UNKNOWN"
            val message = errorNode["message"]?.asText() ?: "결제 승인 실패"
            throw TossPaymentException(code, message)
        }

        return parseTossPaymentResponse(response.body!!)
    }

    /**
     * 결제 조회 (GET /v1/payments/{paymentKey})
     */
    fun getPayment(paymentKey: String): TossPaymentResponse {
        val url = "$baseUrl/v1/payments/$paymentKey"
        val headers = createHeaders()

        val response = try {
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, headers),
                String::class.java,
            )
        } catch (e: HttpClientErrorException) {
            val errorBody = e.responseBodyAsString
            val errorNode = objectMapper.readTree(errorBody)
            val code = errorNode["code"]?.asText() ?: "UNKNOWN"
            val message = errorNode["message"]?.asText() ?: "결제 조회 실패"
            throw TossPaymentException(code, message)
        }

        return parseTossPaymentResponse(response.body!!)
    }

    /**
     * orderId로 결제 조회 (GET /v1/payments/orders/{orderId})
     */
    fun getPaymentByOrderId(orderId: String): TossPaymentResponse {
        val url = "$baseUrl/v1/payments/orders/$orderId"
        val headers = createHeaders()

        val response = try {
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, headers),
                String::class.java,
            )
        } catch (e: HttpClientErrorException) {
            val errorBody = e.responseBodyAsString
            val errorNode = objectMapper.readTree(errorBody)
            val code = errorNode["code"]?.asText() ?: "UNKNOWN"
            val message = errorNode["message"]?.asText() ?: "결제 조회 실패"
            throw TossPaymentException(code, message)
        }

        return parseTossPaymentResponse(response.body!!)
    }

    /**
     * 결제 취소 (POST /v1/payments/{paymentKey}/cancel)
     */
    fun cancelPayment(paymentKey: String, cancelReason: String): TossPaymentResponse {
        val url = "$baseUrl/v1/payments/$paymentKey/cancel"
        val headers = createHeaders()

        val body = mapOf(
            "cancelReason" to cancelReason,
        )

        val response = try {
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(body, headers),
                String::class.java,
            )
        } catch (e: HttpClientErrorException) {
            val errorBody = e.responseBodyAsString
            val errorNode = objectMapper.readTree(errorBody)
            val code = errorNode["code"]?.asText() ?: "UNKNOWN"
            val message = errorNode["message"]?.asText() ?: "결제 취소 실패"
            throw TossPaymentException(code, message)
        }

        return parseTossPaymentResponse(response.body!!)
    }

    private fun parseTossPaymentResponse(responseBody: String): TossPaymentResponse {
        val json = objectMapper.readTree(responseBody)
        return TossPaymentResponse(
            paymentKey = json["paymentKey"]?.asText() ?: "",
            orderId = json["orderId"]?.asText() ?: "",
            status = json["status"]?.asText() ?: "UNKNOWN",
            totalAmount = json["totalAmount"]?.asLong() ?: 0L,
            method = json["method"]?.asText(),
            requestedAt = json["requestedAt"]?.asText(),
            approvedAt = json["approvedAt"]?.asText(),
            responseJson = responseBody,
        )
    }

    private fun createHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        val encoded = Base64.getEncoder().encodeToString("$secretKey:".toByteArray())
        set("Authorization", "Basic $encoded")
    }
}

data class TossPaymentResponse(
    val paymentKey: String,
    val orderId: String,
    val status: String,
    val totalAmount: Long,
    val method: String?,
    val requestedAt: String?,
    val approvedAt: String?,
    val responseJson: String,
)

class TossPaymentException(val code: String, override val message: String) : RuntimeException("[$code] $message")
