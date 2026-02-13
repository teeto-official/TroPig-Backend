package com.tropig.backend.payment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.service.ContentService
import com.tropig.backend.member.enums.Role
import com.tropig.backend.payment.enums.PaymentStatus
import com.tropig.backend.payment.enums.PurchaseStatus
import com.tropig.backend.payment.model.response.RevenueItemResponse
import com.tropig.backend.payment.model.response.RevenueSummaryResponse
import com.tropig.backend.payment.repository.CreatorSettlementRepository
import com.tropig.backend.payment.repository.PaymentRepository
import com.tropig.backend.payment.repository.PurchaseRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class RevenueService(
    private val contentRepository: ContentRepository,
    private val purchaseRepository: PurchaseRepository,
    private val paymentRepository: PaymentRepository,
    private val creatorSettlementRepository: CreatorSettlementRepository,
) {

    private fun validateCreator(role: Role) {
        if (role != Role.CREATOR) {
            throw MemberException(
                message = "CREATOR 권한이 필요한 요청입니다.",
                code = MessageCode.INCORRECT_ROLE,
            )
        }
    }

    /**
     * 수익 리스트 조회
     * - 본인이 등록한 PUBLISHED, PRIVATE 작품
     * - 해당 작품에 대해 구매완료(PurchaseStatus.COMPLETED & PaymentStatus.PAID) 이력 기준
     */
    fun getRevenueItems(authMember: AuthMember, contents: List<Content>): List<RevenueItemResponse> {
        validateCreator(authMember.role)

        val contentById = contents.associateBy { it.id }
        val contentIds = contents.map { it.id }

        val purchases = purchaseRepository.findByContentIdInAndStatus(
            contentIds = contentIds,
            status = PurchaseStatus.COMPLETED,
        )

        if (purchases.isEmpty()) {
            return emptyList()
        }

        val paymentIds = purchases.map { it.paymentId }.distinct()
        val payments = paymentRepository.findAllById(paymentIds)
            .filter { it.status == PaymentStatus.PAID }
            .associateBy { it.id }

        return purchases.mapNotNull { purchase ->
            val content = contentById[purchase.contentId] ?: return@mapNotNull null
            val payment = payments[purchase.paymentId] ?: return@mapNotNull null

            RevenueItemResponse(
                title = content.title,
                price = payment.amount,
                paymentCreatedAt = payment.createdAt,
                paymentId = payment.id,
            )
        }
    }

    /**
     * CREATOR가 가진 작품 목록 (PUBLISHED, PRIVATE)을 memberId 기준으로 30분 캐싱
     */
    @Cacheable(cacheNames = ["creatorContentsByMember"], key = "#memberId + '-' + #type.name()")
    fun getCreatorContents(memberId: Long, type: ContentType): List<Content> {
        return contentRepository.findByMemberIdAndTypeAndStatusIn(
            memberId = memberId,
            type = type,
            status = ContentsStatus.purchasedStatuses,
        )
    }

    /**
     * CREATOR가 가진 작품 목록 (PUBLISHED, PRIVATE)을 memberId 기준으로 30분 캐싱
     */
    @Cacheable(cacheNames = ["creatorContentsByMember"], key = "#memberId + '-' + #type.name()")
    fun getAllCreatorContents(memberId: Long): List<Content> {
        return contentRepository.findByMemberIdAndStatusIn(
            memberId = memberId,
            status = ContentsStatus.purchasedStatuses,
        )
    }

    /**
     * 전체 수익 조회
     * - 본인이 등록한 작품 기준 전체 수익
     * - 출금완료 내역(creator_withdrawal)에 대해서는 차감
     * - memberId 기준으로 10분 캐싱
     */
    @Cacheable(cacheNames = ["revenueSummaryByMember"], key = "#authMember.memberId")
    fun getRevenueSummary(authMember: AuthMember, contents: List<Content>): RevenueSummaryResponse {
        validateCreator(authMember.role)

        val contentIds = contents.map { it.id }

        val purchases = purchaseRepository.findByContentIdInAndStatus(
            contentIds = contentIds,
            status = PurchaseStatus.COMPLETED,
        )

        val paymentIds = purchases.map { it.paymentId }.distinct()
        val totalRevenue = if (paymentIds.isEmpty()) {
            0L
        } else {
            paymentRepository.findAllById(paymentIds)
                .filter { it.status == PaymentStatus.PAID }
                .sumOf { it.amount }
        }

        val withdrawnAmount = creatorSettlementRepository.findByMemberId(authMember.memberId)
            .sumOf { it.amount }

        val availableRevenue = totalRevenue - withdrawnAmount

        return RevenueSummaryResponse(
            totalRevenue = totalRevenue,
            withdrawnAmount = withdrawnAmount,
            availableRevenue = availableRevenue,
        )
    }
}

