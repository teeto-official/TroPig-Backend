package com.tropig.backend.payment.service

import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.payment.enums.PurchaseStatus
import com.tropig.backend.payment.model.request.PurchasedContentListRequest
import com.tropig.backend.payment.model.result.PurchasedContentProjection
import com.tropig.backend.payment.repository.PurchaseRepository
import org.springframework.stereotype.Service

@Service
class PaymentContentService(private val purchaseRepository: PurchaseRepository) {
    /**
     * 특정 회원이 특정 작품을 구매했는지 여부를 확인합니다.
     * @param memberId 회원 ID
     * @param contentId 작품 ID
     * @return 구매 완료 상태인 경우 true, 그렇지 않으면 false
     */
    fun isContentPurchased(memberId: Long, contentId: Long): Boolean =
        purchaseRepository.existsByMemberIdAndContentIdAndStatus(
            memberId = memberId,
            contentId = contentId,
            status = PurchaseStatus.COMPLETED,
        )

    fun getPurchaseCount(memberId: Long): Long =
        purchaseRepository.countByMemberIdAndStatus(memberId, PurchaseStatus.COMPLETED)

    fun getPurchasedContents(
        memberId: Long,
        request: PurchasedContentListRequest,
    ): CursorSlice<PurchasedContentProjection> = purchaseRepository.findPurchasedContents(memberId, request)
}
