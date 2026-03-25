package com.tropig.backend.payment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.payment.enums.PaymentStatus
import com.tropig.backend.payment.enums.PurchaseStatus
import com.tropig.backend.payment.enums.WithdrawalStatus
import com.tropig.backend.payment.model.request.RevenueListRequest
import com.tropig.backend.payment.model.request.WithdrawalListRequest
import com.tropig.backend.payment.model.response.RevenueItemResponse
import com.tropig.backend.payment.model.response.RevenueSummaryResponse
import com.tropig.backend.payment.model.response.WithdrawalItemResponse
import com.tropig.backend.payment.repository.CreatorSettlementRepository
import com.tropig.backend.payment.repository.PaymentRepository
import com.tropig.backend.payment.repository.PurchaseRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class RevenueService(
    private val contentRepository: ContentRepository,
    private val purchaseRepository: PurchaseRepository,
    private val paymentRepository: PaymentRepository,
    private val creatorSettlementRepository: CreatorSettlementRepository,
    private val memberRepository: MemberRepository,
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
    fun getRevenueItems(
        authMember: AuthMember,
        contents: List<Content>,
        request: RevenueListRequest,
    ): CursorSlice<RevenueItemResponse> {
        validateCreator(authMember.role)

        val paidContents = contents.filter { it.price > 0 }
        if (paidContents.isEmpty()) {
            return CursorSlice(items = emptyList(), hasNext = false)
        }

        val contentById = paidContents.associateBy { it.id }
        val contentIds = contentById.keys.toList()
        val pageable = PageRequest.of(0, request.size + 1)

        val purchases = if (request.cursorCreatedAt != null) {
            purchaseRepository.findByContentIdInAndStatusWithCursor(
                contentIds = contentIds,
                status = PurchaseStatus.COMPLETED,
                cursorCreatedAt = request.cursorCreatedAt,
                cursorId = request.cursorId,
                size = request.size + 1,
            )
        } else {
            purchaseRepository.findByContentIdInAndStatusOrderByCreatedAtDesc(
                contentIds = contentIds,
                status = PurchaseStatus.COMPLETED,
                pageable = pageable,
            )
        }

        if (purchases.isEmpty()) {
            return CursorSlice(items = emptyList(), hasNext = false)
        }

        val hasNext = purchases.size > request.size
        val pagedPurchases = purchases.take(request.size)

        val purchaserIds = pagedPurchases.map { it.memberId }.distinct()
        val purchaserById = memberRepository.findAllById(purchaserIds).associateBy { it.id }

        val items = pagedPurchases.mapNotNull { purchase ->
            val content = contentById[purchase.contentId] ?: return@mapNotNull null
            val purchaser = purchaserById[purchase.memberId] ?: return@mapNotNull null

            RevenueItemResponse(
                id = purchase.id,
                title = content.title,
                purchasedAt = purchase.createdAt,
                purchaserNickname = purchaser.nickname,
                amount = purchase.amount,
            )
        }

        val lastPurchase = pagedPurchases.lastOrNull()

        return CursorSlice(
            items = items,
            hasNext = hasNext,
            nextCursorId = lastPurchase?.id,
            nextCursorDateAt = lastPurchase?.createdAt,
        )
    }

    /**
     * CREATOR가 가진 작품 목록 (PUBLISHED, PRIVATE)을 memberId 기준으로 30분 캐싱
     */
//    @Cacheable(cacheNames = ["creatorContentsByMember"], key = "#memberId + '-' + #type.name()")
    fun getCreatorContents(memberId: Long, type: ContentType): List<Content> =
        contentRepository.findByMemberIdAndTypeAndStatusIn(
            memberId = memberId,
            type = type,
            status = ContentsStatus.purchasedStatuses,
        )

    /**
     * CREATOR가 가진 작품 목록 (PUBLISHED, PRIVATE)을 memberId 기준으로 30분 캐싱
     */
    @Cacheable(cacheNames = ["creatorAllContentsByMember"], key = "#memberId")
    fun getAllCreatorContents(memberId: Long): List<Content> = contentRepository.findByMemberIdAndStatusIn(
        memberId = memberId,
        status = ContentsStatus.purchasedStatuses,
    )

    /**
     * 전체 수익 조회
     * - 본인이 등록한 작품 기준 전체 수익
     * - 출금완료 내역(creator_withdrawal)에 대해서는 차감
     * - memberId 기준으로 10분 캐싱 (TODO: Redis 캐싱으로 변경)
     */
//    @Cacheable(cacheNames = ["revenueSummaryByMember"], key = "#authMember.memberId")
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

    fun getWithdrawalList(memberId: Long, request: WithdrawalListRequest): CursorSlice<WithdrawalItemResponse> {
        val pageable = PageRequest.of(0, request.size + 1)

        val settlements = if (request.cursorCreatedAt != null) {
            creatorSettlementRepository.findByMemberIdWithCursor(
                memberId = memberId,
                cursorCreatedAt = request.cursorCreatedAt,
                cursorId = request.cursorId,
                size = request.size + 1,
            )
        } else {
            creatorSettlementRepository.findByMemberIdOrderByCreatedAtDesc(
                memberId = memberId,
                pageable = pageable,
            )
        }

        val hasNext = settlements.size > request.size
        val items = settlements.take(request.size)

        val lastItem = items.lastOrNull()

        return CursorSlice(
            items = items.map { settlement ->
                WithdrawalItemResponse(
                    id = settlement.id,
                    amount = settlement.amount,
                    createdAt = settlement.createdAt,
                    withdrawalStatus = WithdrawalStatus.COMPLETED,
                )
            },
            hasNext = hasNext,
            nextCursorId = lastItem?.id,
            nextCursorDateAt = lastItem?.createdAt,
        )
    }
}
