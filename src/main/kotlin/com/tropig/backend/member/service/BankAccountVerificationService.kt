package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.member.enums.BankCode
import com.tropig.backend.partner.client.PortOnePartnerClient
import com.tropig.backend.partner.client.PortOnePartnerException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 은행 계좌 인증 서비스
 * PortOne Bank Account API를 사용하여 계좌 실명 인증
 */
@Service
class BankAccountVerificationService(
    private val portOnePartnerClient: PortOnePartnerClient
) {
    private val logger = LoggerFactory.getLogger(BankAccountVerificationService::class.java)

    /**
     * 은행 계좌 인증
     * @param bankName 한글 은행명 (예: "신한은행")
     * @param accountNumber 계좌번호 (숫자만)
     * @param expectedHolder 예상 예금주명
     * @return 인증 결과
     */
    fun verifyBankAccount(
        bankName: String,
        accountNumber: String,
        expectedHolder: String
    ): BankAccountVerificationResult {
        // Convert Korean bank name to PortOne bank code
        val bankCode = try {
            BankCode.toPortOneCode(bankName)
        } catch (e: IllegalArgumentException) {
            logger.error("Unsupported bank: $bankName")
            throw MemberException(
                "지원하지 않는 은행입니다: $bankName",
                MessageCode.UNSUPPORTED_BANK
            )
        }

        // Call PortOne Bank Account API
        val accountHolder = try {
            portOnePartnerClient.getBankAccountHolder(bankCode, accountNumber)
        } catch (e: PortOnePartnerException) {
            logger.error("Bank account verification failed", e)
            throw MemberException(
                "계좌 인증에 실패했습니다.",
                MessageCode.BANK_ACCOUNT_VERIFICATION_FAILED
            )
        }

        // Compare account holder names (normalize for comparison)
        val normalized1 = normalizeKoreanName(accountHolder)
        val normalized2 = normalizeKoreanName(expectedHolder)

        if (normalized1 != normalized2) {
            logger.warn("Account holder mismatch: expected=$expectedHolder, actual=$accountHolder")
            return BankAccountVerificationResult(
                verified = false,
                accountHolder = accountHolder,
                message = "예금주명이 일치하지 않습니다."
            )
        }

        return BankAccountVerificationResult(
            verified = true,
            accountHolder = accountHolder,
            message = "계좌 인증 완료"
        )
    }

    /**
     * 한글 이름 정규화 (공백, 특수문자 제거)
     */
    private fun normalizeKoreanName(name: String): String {
        return name.replace(Regex("[\\s\\-\\.]"), "")
    }
}

/**
 * 은행 계좌 인증 결과
 */
data class BankAccountVerificationResult(
    val verified: Boolean,
    val accountHolder: String,
    val message: String
)
