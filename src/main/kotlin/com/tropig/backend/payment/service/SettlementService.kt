package com.tropig.backend.payment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.exception.PaymentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.enums.Role
import com.tropig.backend.partner.enums.PartnerStatus
import com.tropig.backend.partner.repository.PartnerRepository
import com.tropig.backend.payment.entity.CreatorSettlement
import com.tropig.backend.payment.enums.WithdrawalStatus
import com.tropig.backend.payment.model.request.CreateSettlementRequest
import com.tropig.backend.payment.model.response.RevenueSummaryResponse
import com.tropig.backend.payment.model.response.SettlementResponse
import com.tropig.backend.payment.repository.CreatorSettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SettlementService(
    private val partnerRepository: PartnerRepository,
    private val creatorSettlementRepository: CreatorSettlementRepository,
    private val revenueService: RevenueService,
) {

    /**
     * 정산 처리
     * 1. 정산 가능 금액 확인
     * 2. 파트너 정보 확인
     * 3. CreatorSettlement에 정산 내역 저장 (PENDING 상태)
     */
    @Transactional
    fun createSettlement(authMember: AuthMember, request: CreateSettlementRequest): SettlementResponse {
        if (authMember.role != Role.CREATOR) {
            throw MemberException(
                "CREATOR 권한이 필요한 요청입니다.",
                MessageCode.INCORRECT_ROLE,
            )
        }

        val partner = partnerRepository.findByMemberId(authMember.memberId)
            ?: throw NotFoundException(
                "파트너로 등록되지 않았습니다.",
                MessageCode.NOT_FOUND_MEMBER,
            )

        if (partner.status != PartnerStatus.ACTIVE) {
            throw PaymentException(
                "파트너가 ACTIVE 상태가 아닙니다. 현재 상태: ${partner.status}",
                MessageCode.PARTNER_REGISTRATION_FAILED,
            )
        }

        val contents = revenueService.getAllCreatorContents(authMember.memberId)
        val revenueSummary = revenueService.getRevenueSummary(authMember, contents)

        if (request.settlementAmount > revenueSummary.availableRevenue) {
            throw PaymentException(
                "정산 가능 금액을 초과했습니다. 정산 가능 금액: ${revenueSummary.availableRevenue}원, 요청 금액: ${request.settlementAmount}원",
                MessageCode.PAYMENT_ERROR,
            )
        }

        if (request.settlementAmount <= 0) {
            throw PaymentException(
                "정산 금액은 1원 이상이어야 합니다.",
                MessageCode.PAYMENT_ERROR,
            )
        }

        val settlementDate = if (request.settlementDate != null) {
            if (!request.settlementDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                throw PaymentException(
                    "정산 일자는 yyyy-MM-dd 형식이어야 합니다.",
                    MessageCode.PAYMENT_ERROR,
                )
            }
            request.settlementDate
        } else {
            LocalDate.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        val memo = request.memo
            ?: "${
                LocalDate.now(ZoneId.of("Asia/Seoul")).year
            }년 ${LocalDate.now(ZoneId.of("Asia/Seoul")).monthValue}월 정산"

        val settlement = CreatorSettlement(
            memberId = authMember.memberId,
            amount = request.settlementAmount,
            description = memo,
            status = WithdrawalStatus.PENDING,
            settlementDate = settlementDate,
        )
        val saved = creatorSettlementRepository.save(settlement)

        return SettlementResponse(
            id = saved.id,
            memberId = authMember.memberId,
            settlementAmount = saved.amount,
            status = saved.status.name,
            settlementDate = settlementDate,
            memo = memo,
            createdAt = saved.createdAt,
        )
    }

    /**
     * 정산 상태 완료 처리 (관리자용)
     */
    @Transactional
    fun completeSettlement(settlementId: Long): SettlementResponse {
        val settlement = creatorSettlementRepository.findById(settlementId).orElseThrow {
            NotFoundException("정산 내역을 찾을 수 없습니다: $settlementId", MessageCode.PAYMENT_ERROR)
        }
        settlement.status = WithdrawalStatus.COMPLETED
        creatorSettlementRepository.save(settlement)

        return toSettlementResponse(settlement)
    }

    /**
     * 정산 상태 실패 처리 (관리자용)
     */
    @Transactional
    fun failSettlement(settlementId: Long, reason: String?): SettlementResponse {
        val settlement = creatorSettlementRepository.findById(settlementId).orElseThrow {
            NotFoundException("정산 내역을 찾을 수 없습니다: $settlementId", MessageCode.PAYMENT_ERROR)
        }
        settlement.status = WithdrawalStatus.FAILED
        if (reason != null) {
            settlement.description = "${settlement.description} (실패 사유: $reason)"
        }
        creatorSettlementRepository.save(settlement)

        return toSettlementResponse(settlement)
    }

    private fun toSettlementResponse(settlement: CreatorSettlement) = SettlementResponse(
        id = settlement.id,
        memberId = settlement.memberId,
        settlementAmount = settlement.amount,
        status = settlement.status.name,
        settlementDate = settlement.settlementDate ?: "",
        memo = settlement.description,
        createdAt = settlement.createdAt,
    )

    /**
     * 정산 가능 금액 조회
     */
    fun getAvailableSettlementAmount(authMember: AuthMember): RevenueSummaryResponse {
        if (authMember.role != Role.CREATOR) {
            throw MemberException(
                "CREATOR 권한이 필요한 요청입니다.",
                MessageCode.INCORRECT_ROLE,
            )
        }

        val contents = revenueService.getAllCreatorContents(authMember.memberId)
        return revenueService.getRevenueSummary(authMember, contents)
    }
}
