package com.tropig.backend.member.service

import com.tropig.backend.client.PortOneClient
import com.tropig.backend.common.enums.BankCode
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.AuthenticatedException
import com.tropig.backend.config.PortOneProperties
import com.tropig.backend.member.model.request.AccountAuthRequest
import com.tropig.backend.member.model.response.AccountAuthResponse
import com.tropig.backend.member.model.response.IdentityVerificationResponse
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AuthService(
    private val memberService: MemberService,
    private val portOneClient: PortOneClient,
) {
    @Transactional
    suspend fun updateAccountInfo(
        memberId: Long,
        request: AccountAuthRequest,
    ): AccountAuthResponse {
        memberService.getUserById(memberId)
        val memberAuthInfo = memberService.findOrCreateForUpdate(memberId)
        return if (memberAuthInfo.authUserAt != null && memberAuthInfo.authUserAt!!.plusYears(1) >= LocalDateTime.now()) {
            val account = verifyAccount(request)
            memberService.authenticateAccount(memberAuthInfo, account)
            account
        } else {
            throw AuthenticatedException(
                message = "${MessageCode.UNAUTHENTICATED_ADULT.name} memberId: $memberId",
                code = MessageCode.UNAUTHENTICATED_ADULT
            )
        }
    }

    @Transactional
    suspend fun updateAdultInfo(memberId: Long, identityVerificationId: String): Boolean {
        memberService.getUserById(memberId)
        val memberAuthInfo = memberService.findOrCreateForUpdate(memberId)
        return if (memberAuthInfo.authUserAt == null || memberAuthInfo.authUserAt!!.plusYears(1) >= LocalDateTime.now()) {
            val verified = verifyIdentity(identityVerificationId)
            memberService.authenticateUser(memberAuthInfo, memberId, verified)
            true
        } else {
            throw AuthenticatedException(
                message = "${MessageCode.UNAUTHENTICATED_ADULT.name} memberId: $memberId",
                code = MessageCode.UNAUTHENTICATED_ADULT
            )
        }

    }

    suspend fun verifyAccount(request: AccountAuthRequest): AccountAuthResponse {
        val holderRes = portOneClient.getBankAccountHolder(
            bank = request.bank,
            accountNumber = request.accountNumber,
            birthdate = request.birthdate,
            businessRegistrationNumber = request.businessRegistrationNumber,
        )

        return AccountAuthResponse(
            holderName = holderRes.holder,
            bankName = request.bank,
            bankAccount = request.accountNumber
        )
    }

    suspend fun verifyIdentity(identityVerificationId: String): IdentityVerificationResponse {
        val res = portOneClient.getIdentityVerification(identityVerificationId)
        require(res.status == "VERIFIED") {
            "본인 인증 실패 (status=${res.status})"
        }

        return res
    }
}