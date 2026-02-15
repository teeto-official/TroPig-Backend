package com.tropig.backend.payment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.exception.PaymentException
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.payment.client.ConfirmPaymentRequest
import com.tropig.backend.payment.client.PortOneApiException
import com.tropig.backend.payment.client.PortOnePaymentClient
import com.tropig.backend.payment.entity.Payment
import com.tropig.backend.payment.entity.Purchase
import com.tropig.backend.payment.enums.PaymentStatus
import com.tropig.backend.payment.enums.PurchaseStatus
import com.tropig.backend.payment.model.request.ConfirmPurchaseRequest
import com.tropig.backend.payment.model.request.CreatePurchaseRequest
import com.tropig.backend.payment.model.response.CreatePurchaseResponse
import com.tropig.backend.payment.model.response.PurchaseResponse
import com.tropig.backend.payment.repository.PaymentRepository
import com.tropig.backend.payment.repository.PurchaseRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentService(
    private val portOnePaymentClient: PortOnePaymentClient,
    private val paymentRepository: PaymentRepository,
    private val purchaseRepository: PurchaseRepository,
    private val contentRepository: ContentRepository,
    @Value("\${portone.store-id}")
    private val storeId: String,
) {
    
    /**
     * 구매 요청 생성 (결제 생성)
     */
    @Transactional
    fun createPurchase(memberId: Long, adult: Boolean, request: CreatePurchaseRequest): CreatePurchaseResponse {
        // 1. 콘텐츠 조회 및 검증
        val content = contentRepository.findByIdAndStatus(request.contentId, ContentsStatus.PUBLISHED)?.let {
            // adult가 false일 때, it.adult가 true인 경우에는 콘텐츠 구매가 안되게 하고 싶어
            if (!adult && it.adult) {
                throw PaymentException(
                    "성인 인증이 필요한 콘텐츠입니다.",
                    MessageCode.ADULT_CONTENT
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
                MessageCode.NOT_FOUND_CONTENT_INFO
            )
        
        // 2. 이미 구매한 콘텐츠인지 확인
        val existingPurchase = purchaseRepository.findByMemberIdAndContentId(memberId, request.contentId)
        if (existingPurchase != null && existingPurchase.status == PurchaseStatus.COMPLETED) {
            throw PaymentException(
                "이미 구매한 콘텐츠입니다.",
                MessageCode.ALREADY_PURCHASED
            )
        }

        // 4. 결제 ID 생성 (PortOne v2는 서버에서 paymentId를 생성)
        val paymentId = generateOrderId(memberId, request.contentId)
        
        // 5. Payment 엔티티 저장 (결제는 프론트엔드에서 PortOne SDK로 시작됨)
        val payment = Payment(
            memberId = memberId,
            portonePaymentId = paymentId,
            amount = content.price.toLong(),
            status = PaymentStatus.PENDING,
            currency = "KRW",
            channelKey = request.channel.channelKey,
            storeId = storeId,
            portoneResponse = null // 결제 생성 시점에는 아직 PortOne 응답 없음
        )
        val savedPayment = paymentRepository.save(payment)
        
        // 6. Purchase 엔티티 저장
        val purchase = Purchase(
            memberId = memberId,
            contentId = request.contentId,
            paymentId = savedPayment.id,
            portonePaymentId = paymentId,
            amount = content.price.toLong(),
            status = PurchaseStatus.PENDING
        )
        val savedPurchase = purchaseRepository.save(purchase)
        
        return CreatePurchaseResponse(
            purchaseId = savedPurchase.id,
            paymentId = savedPayment.id,
            portonePaymentId = paymentId,
            amount = content.price.toLong()
        )
    }
    
    /**
     * 결제 승인
     */
    @Transactional
    fun confirmPurchase(memberId: Long, request: ConfirmPurchaseRequest): PurchaseResponse {
        // 1. Payment 조회
        val payment = paymentRepository.findByPortonePaymentId(request.portonePaymentId)
            ?: throw NotFoundException(
                "결제를 찾을 수 없습니다: ${request.portonePaymentId}",
                MessageCode.NOT_FOUND_PAYMENT_INFO,
            )
        
        // 2. 권한 확인
        if (payment.memberId != memberId) {
            throw PaymentException(
                "본인의 결제만 확인할 수 있습니다.",
                MessageCode.NOT_OWN_PAYMENT_INFO
            )
        }
        
        // 3. 포트원 결제 승인
        val confirmRequest = ConfirmPaymentRequest(
            storeId = storeId,
            receiptId = request.receiptId
        )
        
        val confirmResponse = try {
            portOnePaymentClient.confirmPayment(request.portonePaymentId, confirmRequest)
        } catch (e: PortOneApiException) {
            // 결제 실패 처리
            payment.status = PaymentStatus.FAILED
            payment.failureReason = e.message
            paymentRepository.save(payment)
            
            // Purchase 상태도 업데이트
            val purchase = purchaseRepository.findByMemberIdAndPaymentId(memberId, payment.id)
            purchase?.let {
                it.status = PurchaseStatus.CANCELLED
                purchaseRepository.save(it)
            }
            
            throw e
        }
        
        // 4. Payment 상태 업데이트
        payment.status = PaymentStatus.PAID
        payment.method = confirmResponse.status
        payment.portoneResponse = confirmResponse.responseJson
        val updatedPayment = paymentRepository.save(payment)
        
        // 5. Purchase 상태 업데이트
        val purchase = purchaseRepository.findByMemberIdAndPaymentId(memberId, payment.id)
            ?: throw NotFoundException(
                "구매 내역을 찾을 수 없습니다.",
                MessageCode.NOT_FOUND_PURCHASE_INFO,
            )
        
        purchase.status = PurchaseStatus.COMPLETED
        val updatedPurchase = purchaseRepository.save(purchase)
        
        return PurchaseResponse(
            id = updatedPurchase.id,
            memberId = updatedPurchase.memberId,
            contentId = updatedPurchase.contentId,
            paymentId = updatedPayment.id,
            amount = updatedPayment.amount,
            status = updatedPurchase.status,
            paymentStatus = updatedPayment.status,
            portonePaymentId = updatedPurchase.portonePaymentId,
            createdAt = updatedPurchase.createdAt,
            updatedAt = updatedPurchase.updatedAt
        )
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
                    code = MessageCode.NOT_FOUND_PAYMENT_INFO
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
            portonePaymentId = purchase.portonePaymentId,
            createdAt = purchase.createdAt,
            updatedAt = purchase.updatedAt
        )
    }
    
    /**
     * 콘텐츠 구매 여부 확인
     */
    fun isContentPurchased(memberId: Long, contentId: Long): Boolean {
        return purchaseRepository.existsByMemberIdAndContentIdAndStatus(
            memberId, 
            contentId, 
            PurchaseStatus.COMPLETED
        )
    }
    
    private fun generateOrderId(memberId: Long, contentId: Long): String {
        return "payment-content-${contentId}-${System.currentTimeMillis()}"
    }
}