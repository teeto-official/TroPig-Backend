package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.partner.client.PortOnePartnerClient
import com.tropig.backend.partner.client.PortOnePartnerException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

/**
 * BankAccountVerificationService 단위 테스트
 */
@ExtendWith(MockitoExtension::class)
class BankAccountVerificationServiceTest {

    @Mock
    private lateinit var portOnePartnerClient: PortOnePartnerClient

    @InjectMocks
    private lateinit var bankAccountVerificationService: BankAccountVerificationService

    @Test
    fun `should verify valid bank account successfully`() {
        // Given
        val bankName = "신한은행"
        val accountNumber = "1234567890"
        val expectedHolder = "홍길동"

        `when`(portOnePartnerClient.getBankAccountHolder("SHINHAN", accountNumber))
            .thenReturn("홍길동")

        // When
        val result = bankAccountVerificationService.verifyBankAccount(
            bankName, accountNumber, expectedHolder
        )

        // Then
        assertTrue(result.verified)
        assertEquals("홍길동", result.accountHolder)
        assertEquals("계좌 인증 완료", result.message)
    }

    @Test
    fun `should fail when account holder name mismatch`() {
        // Given
        val bankName = "신한은행"
        val accountNumber = "1234567890"
        val expectedHolder = "홍길동"

        `when`(portOnePartnerClient.getBankAccountHolder("SHINHAN", accountNumber))
            .thenReturn("김철수")

        // When
        val result = bankAccountVerificationService.verifyBankAccount(
            bankName, accountNumber, expectedHolder
        )

        // Then
        assertFalse(result.verified)
        assertEquals("김철수", result.accountHolder)
        assertEquals("예금주명이 일치하지 않습니다.", result.message)
    }

    @Test
    fun `should handle name with spaces correctly`() {
        // Given
        val bankName = "신한은행"
        val accountNumber = "1234567890"
        val expectedHolder = "홍 길 동"

        `when`(portOnePartnerClient.getBankAccountHolder("SHINHAN", accountNumber))
            .thenReturn("홍길동")

        // When
        val result = bankAccountVerificationService.verifyBankAccount(
            bankName, accountNumber, expectedHolder
        )

        // Then
        assertTrue(result.verified, "Should match after normalizing spaces")
    }

    @Test
    fun `should throw exception for unsupported bank`() {
        // Given
        val bankName = "존재하지않는은행"
        val accountNumber = "1234567890"
        val expectedHolder = "홍길동"

        // When & Then
        val exception = assertThrows<MemberException> {
            bankAccountVerificationService.verifyBankAccount(
                bankName, accountNumber, expectedHolder
            )
        }

        assertEquals(MessageCode.UNSUPPORTED_BANK, exception.code)
        assertTrue(exception.message!!.contains("지원하지 않는 은행"))
    }

    @Test
    fun `should throw exception when PortOne API fails`() {
        // Given
        val bankName = "신한은행"
        val accountNumber = "1234567890"
        val expectedHolder = "홍길동"

        `when`(portOnePartnerClient.getBankAccountHolder("SHINHAN", accountNumber))
            .thenThrow(PortOnePartnerException("API Error"))

        // When & Then
        val exception = assertThrows<MemberException> {
            bankAccountVerificationService.verifyBankAccount(
                bankName, accountNumber, expectedHolder
            )
        }

        assertEquals(MessageCode.BANK_ACCOUNT_VERIFICATION_FAILED, exception.code)
    }

    @Test
    fun `should verify all supported banks`() {
        // Given - Test major banks
        val testCases = listOf(
            "신한은행" to "SHINHAN",
            "국민은행" to "KOOKMIN",
            "우리은행" to "WOORI",
            "하나은행" to "HANA",
            "카카오뱅크" to "KAKAO"
        )

        testCases.forEach { (koreanName, portOneCode) ->
            // When
            `when`(portOnePartnerClient.getBankAccountHolder(portOneCode, "1234567890"))
                .thenReturn("홍길동")

            // Then
            val result = bankAccountVerificationService.verifyBankAccount(
                koreanName, "1234567890", "홍길동"
            )
            assertTrue(result.verified, "Should verify $koreanName")
        }
    }
}
