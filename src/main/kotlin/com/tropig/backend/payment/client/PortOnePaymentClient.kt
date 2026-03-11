package com.tropig.backend.payment.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PortOnePaymentClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${portone.key.secret-v2}") private val apiSecret: String,
    @Value("\${portone.base-url:https://api.portone.io}") private val baseUrl: String,
) {

    /**
     * 결제 생성 (POST /payments) - PortOne v2에서는 사용하지 않음 (서버에서 paymentId 생성)
     * @deprecated PortOne v2는 서버에서 paymentId를 생성하고 프론트엔드에서 결제를 시작합니다.
     */
    @Deprecated("PortOne v2는 서버에서 paymentId를 생성합니다. 이 메서드는 사용하지 않습니다.")
    fun createPayment(request: CreatePaymentRequest): CreatePaymentResponse {
        val url = "$baseUrl/payments"
        val headers = createHeaders()

        val body = mapOf(
            "storeId" to request.storeId,
            "channelKey" to request.channelKey,
            "orderId" to request.orderId,
            "amount" to request.amount,
            "currency" to request.currency,
            "customer" to mapOf(
                "fullName" to request.customerName,
                "phoneNumber" to request.customerPhone,
                "email" to request.customerEmail,
            ),
            "products" to request.products.map { product ->
                mapOf(
                    "name" to product.name,
                    "quantity" to product.quantity,
                    "unitPrice" to product.unitPrice,
                )
            },
            "customData" to request.customData,
        )

        val response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java,
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw PortOneApiException("결제 생성 실패: ${response.statusCode} ${response.body}")
        }

        val responseBody = objectMapper.readTree(response.body)
        return CreatePaymentResponse(
            paymentId = responseBody["payment"]?.get("id")?.asText()
                ?: throw PortOneApiException("결제 ID를 찾을 수 없습니다"),
            responseJson = response.body!!,
        )
    }

    /**
     * 결제 승인 (POST /payments/{paymentId}/confirm)
     * confirm 모드에서 READY 상태의 결제를 PAID로 전환
     */
    fun confirmPayment(paymentId: String, storeId: String, paymentToken: String): PaymentQueryResponse {
        val url = "$baseUrl/payments/$paymentId/confirm"
        val headers = createHeaders()

        val body = mapOf(
            "storeId" to storeId,
            "paymentToken" to paymentToken,
        )

        val response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java,
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw PortOneApiException("결제 승인 실패: ${response.statusCode} ${response.body}")
        }

        val responseBody = objectMapper.readTree(response.body)
        return PaymentQueryResponse(
            paymentId = paymentId,
            status = responseBody["payment"]?.get("status")?.asText() ?: "UNKNOWN",
            amount = responseBody["payment"]?.get("amount")?.get("total")?.asLong() ?: 0L,
            method = responseBody["payment"]?.get("method")?.get("type")?.asText(),
            txId = responseBody["payment"]?.get("id")?.asText(),
            responseJson = response.body!!,
        )
    }

    /**
     * 결제 조회 (GET /payments/{paymentId})
     * 포트원에 결제 상태를 조회하여 검증에 사용
     */
    fun getPayment(paymentId: String, storeId: String): PaymentQueryResponse {
        val url = "$baseUrl/payments/$paymentId?storeId=$storeId"
        val headers = createHeaders()

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(null, headers),
            String::class.java,
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw PortOneApiException("결제 조회 실패: ${response.statusCode} ${response.body}")
        }

        val responseBody = objectMapper.readTree(response.body)
        return PaymentQueryResponse(
            paymentId = paymentId,
            status = responseBody["status"]?.asText() ?: "UNKNOWN",
            amount = responseBody["amount"]?.get("total")?.asLong() ?: 0L,
            method = responseBody["method"]?.get("type")?.asText(),
            txId = responseBody["id"]?.asText(),
            responseJson = response.body!!,
        )
    }

    /**
     * 결제 취소 (POST /payments/{paymentId}/cancel)
     */
    fun cancelPayment(paymentId: String, request: CancelPaymentRequest): CancelPaymentResponse {
        val url = "$baseUrl/payments/$paymentId/cancel"
        val headers = createHeaders()

        val body = mapOf(
            "storeId" to request.storeId,
            "reason" to request.reason,
        )

        val response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java,
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw PortOneApiException("결제 취소 실패: ${response.statusCode} ${response.body}")
        }

        val responseBody = objectMapper.readTree(response.body)
        return CancelPaymentResponse(
            paymentId = paymentId,
            status = responseBody["payment"]?.get("status")?.asText() ?: "UNKNOWN",
            responseJson = response.body!!,
        )
    }

    private fun createHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("Authorization", "PortOne $apiSecret")
    }
}

// Request DTOs
data class CreatePaymentRequest(
    val storeId: String,
    val channelKey: String,
    val orderId: String,
    val amount: Long,
    val currency: String = "KRW",
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val products: List<ProductInfo>,
    val customData: Map<String, Any>? = null,
)

data class ProductInfo(val name: String, val quantity: Int, val unitPrice: Long)

data class CreatePaymentResponse(val paymentId: String, val responseJson: String)

data class PaymentQueryResponse(
    val paymentId: String,
    val status: String,
    val amount: Long,
    val method: String?,
    val txId: String?,
    val responseJson: String,
)

data class CancelPaymentRequest(val storeId: String, val reason: String)

data class CancelPaymentResponse(val paymentId: String, val status: String, val responseJson: String)

class PortOneApiException(message: String) : RuntimeException(message)
