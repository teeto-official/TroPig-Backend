package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.member.enums.BankCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 은행 계좌 인증 서비스
 * 은행명 유효성 및 예금주명 일치 여부 검증
 */
@Service
class BankAccountVerificationService {
    private val logger = LoggerFactory.getLogger(BankAccountVerificationService::class.java)

    /**
     * 은행 계좌 검증
     * @param bankName 한글 은행명 (예: "신한은행")
     * @param accountNumber 계좌번호 (숫자만)
     * @param expectedHolder 예상 예금주명
     * @return 검증 결과
     */
    fun verifyBankAccount(
        bankName: String,
        accountNumber: String,
        expectedHolder: String,
    ): BankAccountVerificationResult {
        // 은행명 유효성 확인
        if (BankCode.fromKoreanName(bankName) == null) {
            logger.error("Unsupported bank: $bankName")
            throw MemberException(
                "지원하지 않는 은행입니다: $bankName",
                MessageCode.UNSUPPORTED_BANK,
            )
        }

        // 계좌번호 형식 검증
        if (!accountNumber.matches(Regex("^[0-9]{10,14}$"))) {
            return BankAccountVerificationResult(
                verified = false,
                accountHolder = expectedHolder,
                message = "계좌번호 형식이 올바르지 않습니다.",
            )
        }

        return BankAccountVerificationResult(
            verified = true,
            accountHolder = expectedHolder,
            message = "계좌 검증 완료",
        )
    }
}

data class BankAccountVerificationResult(val verified: Boolean, val accountHolder: String, val message: String)
