package com.tropig.backend.payment.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.exception.PaymentException
import com.tropig.backend.common.model.AuthMember
import com.tropig.backend.member.enums.Role
import com.tropig.backend.partner.enums.PartnerStatus
import com.tropig.backend.partner.client.PlatformCreateManualTransferRequest
import com.tropig.backend.partner.client.PortOnePlatformApiException
import com.tropig.backend.partner.client.PortOnePlatformClient
import com.tropig.backend.partner.repository.PartnerRepository
import com.tropig.backend.payment.entity.CreatorSettlement
import com.tropig.backend.payment.model.request.CreateSettlementRequest
import com.tropig.backend.payment.model.response.RevenueSummaryResponse
import com.tropig.backend.payment.model.response.SettlementResponse
import com.tropig.backend.payment.repository.CreatorSettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SettlementService(
    private val portOnePlatformClient: PortOnePlatformClient,
    private val partnerRepository: PartnerRepository,
    private val creatorSettlementRepository: CreatorSettlementRepository,
    private val revenueService: RevenueService,
) {
    
    /**
     * 정산 처리
     * 1. 정산 가능 금액 확인
     * 2. 파트너 정보 확인
     * 3. PortOne API로 정산 처리
     * 4. CreatorSettlement에 정산 내역 저장
     */
    @Transactional
    fun createSettlement(
        authMember: AuthMember,
        request: CreateSettlementRequest
    ): SettlementResponse {
        // 1. CREATOR 권한 확인
        if (authMember.role != Role.CREATOR) {
            throw MemberException(
                "CREATOR 권한이 필요한 요청입니다.",
                MessageCode.INCORRECT_ROLE
            )
        }
        
        // 2. 파트너 정보 확인
        val partner = partnerRepository.findByMemberId(authMember.memberId)
            ?: throw NotFoundException(
                "파트너로 등록되지 않았습니다.",
                MessageCode.NOT_FOUND_MEMBER
            )
        
        if (partner.status != PartnerStatus.ACTIVE) {
            throw PaymentException(
                "파트너가 ACTIVE 상태가 아닙니다. 현재 상태: ${partner.status}",
                MessageCode.PARTNER_REGISTRATION_FAILED
            )
        }
        
        // 3. 정산 가능 금액 확인
        val contents = revenueService.getAllCreatorContents(authMember.memberId)
        val revenueSummary = revenueService.getRevenueSummary(authMember, contents)
        
        if (request.settlementAmount > revenueSummary.availableRevenue) {
            throw PaymentException(
                "정산 가능 금액을 초과했습니다. 정산 가능 금액: ${revenueSummary.availableRevenue}원, 요청 금액: ${request.settlementAmount}원",
                MessageCode.PAYMENT_ERROR
            )
        }
        
        if (request.settlementAmount <= 0) {
            throw PaymentException(
                "정산 금액은 1원 이상이어야 합니다.",
                MessageCode.PAYMENT_ERROR
            )
        }
        
        // 4. 정산 일자 설정 및 검증 (null이면 오늘 날짜)
        val settlementDate = if (request.settlementDate != null) {
            // 형식 검증 (yyyy-MM-dd)
            if (!request.settlementDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                throw PaymentException(
                    "정산 일자는 yyyy-MM-dd 형식이어야 합니다.",
                    MessageCode.PAYMENT_ERROR
                )
            }
            request.settlementDate
        } else {
            LocalDate.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        
        // 5. PortOne API로 정산 처리
        val transferRequest = PlatformCreateManualTransferRequest(
            partnerId = partner.portonePartnerId,
            settlementAmount = request.settlementAmount,
            settlementTaxFreeAmount = 0L, // 기본값 0
            settlementDate = settlementDate,
            memo = request.memo ?: "${LocalDate.now(ZoneId.of("Asia/Seoul")).year}년 ${LocalDate.now(ZoneId.of("Asia/Seoul")).monthValue}월 정산"
        )
        
        val transferResponse = try {
            portOnePlatformClient.createManualTransfer(transferRequest)
        } catch (e: PortOnePlatformApiException) {
            throw PaymentException(
                "정산 처리에 실패했습니다: ${e.message}",
                MessageCode.PARTNER_REGISTRATION_FAILED
            )
        }
        
        // 6. CreatorSettlement에 정산 내역 저장 (description에 "정산" 포함)
        val settlement = CreatorSettlement(
            memberId = authMember.memberId,
            amount = request.settlementAmount,
            description = "정산 - ${request.memo ?: "${LocalDate.now(ZoneId.of("Asia/Seoul")).year}년 ${LocalDate.now(ZoneId.of("Asia/Seoul")).monthValue}월 정산"} (Transfer ID: ${transferResponse.id})"
        )
        creatorSettlementRepository.save(settlement)
        
        // 7. 응답 생성
        return SettlementResponse(
            id = transferResponse.id,
            partnerId = transferResponse.partnerId,
            memberId = authMember.memberId,
            settlementAmount = transferResponse.settlementAmount,
            status = transferResponse.status,
            settlementDate = settlementDate,
            memo = request.memo,
            createdAt = transferResponse.createdAt?.let {
                try {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                } catch (e: Exception) {
                    null
                }
            },
            completedAt = null // 정산 완료는 웹훅으로 업데이트
        )
    }
    
    /**
     * 정산 가능 금액 조회
     */
    fun getAvailableSettlementAmount(authMember: AuthMember): RevenueSummaryResponse {
        if (authMember.role != Role.CREATOR) {
            throw MemberException(
                "CREATOR 권한이 필요한 요청입니다.",
                MessageCode.INCORRECT_ROLE
            )
        }
        
        val contents = revenueService.getAllCreatorContents(authMember.memberId)
        return revenueService.getRevenueSummary(authMember, contents)
    }
}
