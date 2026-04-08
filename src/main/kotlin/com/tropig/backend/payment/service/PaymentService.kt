package com.tropig.backend.payment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.exception.PaymentException
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.payment.client.TossPaymentClient
import com.tropig.backend.payment.client.TossPaymentException
import com.tropig.backend.payment.entity.Payment
import com.tropig.backend.payment.entity.Purchase
import com.tropig.backend.payment.enums.PaymentStatus
import com.tropig.backend.payment.enums.PurchaseStatus
import com.tropig.backend.payment.model.request.ConfirmPurchaseRequest
import com.tropig.backend.payment.model.request.CreatePurchaseRequest
import com.tropig.backend.payment.model.request.FailPurchaseRequest
import com.tropig.backend.payment.model.response.CreatePurchaseResponse
import com.tropig.backend.payment.model.response.PurchaseResponse
import com.tropig.backend.payment.repository.PaymentRepository
import com.tropig.backend.payment.repository.PurchaseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentService(
    private val tossPaymentClient: TossPaymentClient,
    private val paymentRepository: PaymentRepository,
    private val purchaseRepository: PurchaseRepository,
    private val contentRepository: ContentRepository,
) {

    /**
     * 구매 요청 생성 (결제 생성)
     */
    @Transactional
    fun createPurchase(memberId: Long, adult: Boolean, request: CreatePurchaseRequest): CreatePurchaseResponse {
        // 1. 콘텐츠 조회 및 검증
        val content = contentRepository.findByIdAndStatus(request.contentId, ContentsStatus.PUBLISHED)?.let {
            if (!adult && it.adult) {
                throw PaymentException(
                    "성인 인증이 필요한 콘텐츠입니다.",
                    MessageCode.ADULT_CONTENT,
                )
            }

            if (it.memberId == memberId) {
                throw PaymentException(
                    "본인의 작품입니다.",
                    MessageCode.OWN_CONTENT,
                )
            }
            it
        }
            ?: throw NotFoundException(
                "콘텐츠를 찾을 수 없습니다: ${request.contentId}",
                MessageCode.NOT_FOUND_CONTENT_INFO,
            )

        // 2. 이미 구매한 콘텐츠인지 확인
        val existingPurchase = purchaseRepository.findByMemberIdAndContentId(memberId, request.contentId)
        if (existingPurchase != null && existingPurchase.status == PurchaseStatus.COMPLETED) {
            throw PaymentException(
                "이미 구매한 콘텐츠입니다.",
                MessageCode.ALREADY_PURCHASED,
            )
        }

        // 3. 주문 ID 생성
        val orderId = generateOrderId(memberId, request.contentId)

        // 4. Payment 엔티티 저장 (결제는 프론트엔드에서 Toss 결제위젯으로 시작됨)
        val payment = Payment(
            memberId = memberId,
            contentId = content.id,
            orderId = orderId,
            amount = content.price.toLong(),
            status = PaymentStatus.PENDING,
            currency = "KRW",
        )
        paymentRepository.save(payment)

        return CreatePurchaseResponse(
            orderId = orderId,
            amount = content.price.toLong(),
        )
    }

    /**
     * 결제 승인
     * Toss Payments는 프론트엔드 결제 완료 후 서버에서 confirm API를 호출해야 최종 승인됨
     */
    @Transactional
    fun confirmPurchase(memberId: Long, request: ConfirmPurchaseRequest): PurchaseResponse {
        // 1. Payment 조회
        val payment = paymentRepository.findByOrderId(request.orderId)
            ?: throw NotFoundException(
                "결제를 찾을 수 없습니다: ${request.orderId}",
                MessageCode.NOT_FOUND_PAYMENT_INFO,
            )

        // 2. 권한 확인
        if (payment.memberId != memberId) {
            throw PaymentException(
                "본인의 결제만 확인할 수 있습니다.",
                MessageCode.NOT_OWN_PAYMENT_INFO,
            )
        }

        // 3. 이미 결제 완료된 건이면 기존 Purchase 반환
        if (payment.status == PaymentStatus.PAID) {
            val existingPurchase = purchaseRepository.findByMemberIdAndPaymentId(memberId, payment.id)
                ?: throw NotFoundException(
                    "구매 내역을 찾을 수 없습니다.",
                    MessageCode.NOT_FOUND_PURCHASE_INFO,
                )
            return PurchaseResponse(
                id = existingPurchase.id,
                memberId = existingPurchase.memberId,
                contentId = existingPurchase.contentId,
                paymentId = payment.id,
                amount = payment.amount,
                status = existingPurchase.status,
                paymentStatus = payment.status,
                createdAt = existingPurchase.createdAt,
                updatedAt = existingPurchase.updatedAt,
            )
        }

        // 4. 결제 금액 검증 (위변조 확인)
        val requestAmount = request.amountAsLong()
        if (requestAmount != payment.amount) {
            payment.status = PaymentStatus.FAILED
            payment.failureReason = "결제 금액 불일치: 예상=${payment.amount}, 실제=$requestAmount"
            paymentRepository.save(payment)
            throw PaymentException(
                "결제 금액이 일치하지 않습니다. 예상: ${payment.amount}원, 실제: ${requestAmount}원",
                MessageCode.PAYMENT_ERROR,
            )
        }

        // 5. Toss 결제 승인 API 호출
        val tossResponse = try {
            tossPaymentClient.confirmPayment(request.paymentKey, request.orderId, requestAmount)
        } catch (e: TossPaymentException) {
            // confirm 실패 시 Toss 조회 API로 실제 승인 여부 확인 (이미 승인된 건일 수 있음)
            val fallbackResponse = try {
                tossPaymentClient.getPaymentByOrderId(request.orderId)
            } catch (queryException: Exception) {
                null
            }

            if (fallbackResponse != null && fallbackResponse.status == "DONE") {
                // Toss에서 이미 승인된 건 → 정상 처리로 진행
                fallbackResponse
            } else {
                payment.status = PaymentStatus.FAILED
                payment.failureReason = "[${e.code}] ${e.message}"
                paymentRepository.save(payment)
                throw PaymentException(
                    "결제 승인에 실패했습니다: ${e.message}",
                    MessageCode.PAYMENT_ERROR,
                )
            }
        }

        // 6. Toss 응답 상태 확인
        if (tossResponse.status != "DONE") {
            payment.status = PaymentStatus.FAILED
            payment.failureReason = "결제 미완료 상태: ${tossResponse.status}"
            paymentRepository.save(payment)
            throw PaymentException(
                "결제가 완료되지 않았습니다. 현재 상태: ${tossResponse.status}",
                MessageCode.PAYMENT_ERROR,
            )
        }

        // 7. Payment 상태 업데이트
        payment.status = PaymentStatus.PAID
        payment.method = tossResponse.method
        payment.paymentKey = tossResponse.paymentKey
        payment.tossResponse = tossResponse.responseJson
        val updatedPayment = paymentRepository.save(payment)

        // 8. Purchase 엔티티 저장
        val purchase = Purchase(
            memberId = memberId,
            contentId = payment.contentId,
            paymentId = payment.id,
            amount = payment.amount,
            status = PurchaseStatus.COMPLETED,
        )
        val savedPurchase = purchaseRepository.save(purchase)

        return PurchaseResponse(
            id = savedPurchase.id,
            memberId = savedPurchase.memberId,
            contentId = savedPurchase.contentId,
            paymentId = updatedPayment.id,
            amount = updatedPayment.amount,
            status = savedPurchase.status,
            paymentStatus = updatedPayment.status,
            createdAt = savedPurchase.createdAt,
            updatedAt = savedPurchase.updatedAt,
        )
    }

    /**
     * 결제 실패 처리
     */
    @Transactional
    fun failPurchase(memberId: Long, request: FailPurchaseRequest) {
        val payment = paymentRepository.findByOrderId(request.orderId)
            ?: throw NotFoundException(
                "결제를 찾을 수 없습니다: ${request.orderId}",
                MessageCode.NOT_FOUND_PAYMENT_INFO,
            )

        if (payment.memberId != memberId) {
            throw PaymentException(
                "본인의 결제만 처리할 수 있습니다.",
                MessageCode.NOT_OWN_PAYMENT_INFO,
            )
        }

        payment.status = PaymentStatus.FAILED
        payment.failureReason = listOfNotNull(request.code, request.message)
            .joinToString(": ")
            .ifEmpty { "결제 실패" }
        paymentRepository.save(payment)
    }

    /**
     * 구매 내역 조회
     */
    fun getPurchase(memberId: Long, purchaseId: Long): PurchaseResponse {
        val purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow {
                NotFoundException(
                    "구매 내역을 찾을 수 없습니다: $purchaseId",
                    MessageCode.NOT_FOUND_PURCHASE_INFO,
                )
            }

        if (purchase.memberId != memberId) {
            throw PaymentException(
                "본인의 구매 내역만 조회할 수 있습니다.",
                MessageCode.NOT_OWN_PAYMENT_INFO,
            )
        }

        val payment = paymentRepository.findById(purchase.paymentId)
            .orElseThrow {
                NotFoundException(
                    message = "결제 정보를 찾을 수 없습니다: ${purchase.paymentId}",
                    code = MessageCode.NOT_FOUND_PAYMENT_INFO,
                )
            }

        return PurchaseResponse(
            id = purchase.id,
            memberId = purchase.memberId,
            contentId = purchase.contentId,
            paymentId = purchase.paymentId,
            amount = purchase.amount,
            status = purchase.status,
            paymentStatus = payment.status,
            createdAt = purchase.createdAt,
            updatedAt = purchase.updatedAt,
        )
    }

    /**
     * 콘텐츠 구매 여부 확인
     */
    fun isContentPurchased(memberId: Long, contentId: Long): Boolean =
        purchaseRepository.existsByMemberIdAndContentIdAndStatus(
            memberId,
            contentId,
            PurchaseStatus.COMPLETED,
        )

    private fun generateOrderId(memberId: Long, contentId: Long): String =
        "payment-content-$contentId-${System.currentTimeMillis()}"
}
