package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.util.EncryptionService
import com.tropig.backend.member.entity.MemberAccount
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.ChangeAccountRequest
import com.tropig.backend.member.model.request.CreatorVerificationRequest
import com.tropig.backend.member.model.response.*
import com.tropig.backend.member.repository.MemberAccountRepository
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import com.tropig.backend.partner.entity.Partner
import com.tropig.backend.partner.enums.PartnerStatus
import com.tropig.backend.partner.repository.PartnerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 창작자 인증 서비스
 * 본인인증 후 은행 계좌 등록 및 파트너 등록
 */
@Service
class CreatorVerificationService(
    private val memberRepository: MemberRepository,
    private val memberAccountRepository: MemberAccountRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val partnerRepository: PartnerRepository,
    private val bankAccountVerificationService: BankAccountVerificationService,
    private val encryptionService: EncryptionService,
) {
    private val logger = LoggerFactory.getLogger(CreatorVerificationService::class.java)

    /**
     * 창작자 인증 (계좌 등록 및 파트너 생성)
     */
    @Transactional
    fun verifyCreator(memberId: Long, request: CreatorVerificationRequest): CreatorVerificationResult {
        // 1. 사전 조건 검증
        val validationData = validateCreatorVerification(memberId, request)

        // 2. 데이터베이스 저장
        return saveCreatorVerification(
            memberId = memberId,
            request = request,
            member = validationData.member,
            existingAccount = validationData.existingAccount,
        )
    }

    private fun validateCreatorVerification(memberId: Long, request: CreatorVerificationRequest): ValidationData {
        val member = memberRepository.findById(memberId).orElseThrow {
            MemberException("회원을 찾을 수 없습니다.", MessageCode.MEMBER_NOT_FOUND)
        }

        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)
            ?: throw MemberException(
                "본인인증이 필요합니다.",
                MessageCode.IDENTITY_VERIFICATION_REQUIRED,
            )

        val existingAccount = memberAccountRepository.findByMemberId(memberId)
        if (existingAccount != null && !existingAccount.isExpired()) {
            throw MemberException(
                "이미 창작자 인증이 완료되었습니다.",
                MessageCode.CREATOR_ALREADY_VERIFIED,
            )
        }

        if (request.accountHolder != authInfo.name) {
            throw MemberException(
                "예금주명이 본인인증 실명과 일치하지 않습니다.",
                MessageCode.ACCOUNT_HOLDER_MISMATCH,
            )
        }

        val verificationResult = bankAccountVerificationService.verifyBankAccount(
            bankName = request.bankName,
            accountNumber = request.accountNumber,
            expectedHolder = request.accountHolder,
        )

        if (!verificationResult.verified) {
            throw MemberException(
                verificationResult.message,
                MessageCode.ACCOUNT_HOLDER_MISMATCH,
            )
        }

        return ValidationData(member, authInfo, existingAccount)
    }

    private fun saveCreatorVerification(
        memberId: Long,
        request: CreatorVerificationRequest,
        member: com.tropig.backend.member.entity.Member,
        existingAccount: MemberAccount?,
    ): CreatorVerificationResult {
        val now = LocalDateTime.now()
        val expiresAt = now.plusYears(MemberAccount.VERIFICATION_VALIDITY_YEARS)
        val encryptedAccountNumber = encryptionService.encrypt(request.accountNumber)

        val memberAccount = if (existingAccount != null) {
            existingAccount.apply {
                this.bankName = request.bankName
                this.accountNumberEncrypted = encryptedAccountNumber
                this.accountHolder = request.accountHolder
                this.verifiedAt = now
                this.expiresAt = expiresAt
                this.lastChangedAt = now
            }
        } else {
            MemberAccount(
                memberId = memberId,
                bankName = request.bankName,
                accountNumberEncrypted = encryptedAccountNumber,
                accountHolder = request.accountHolder,
                verifiedAt = now,
                expiresAt = expiresAt,
                lastChangedAt = now,
            )
        }
        memberAccountRepository.save(memberAccount)

        // Partner 등록 (자체 DB 관리)
        val existingPartner = partnerRepository.findByMemberId(memberId)
        if (existingPartner == null) {
            partnerRepository.save(
                Partner(
                    memberId = memberId,
                    name = request.accountHolder,
                    email = member.email,
                    status = PartnerStatus.ACTIVE,
                ),
            )
        } else {
            existingPartner.status = PartnerStatus.ACTIVE
            partnerRepository.save(existingPartner)
        }

        member.role = Role.CREATOR
        memberRepository.save(member)

        logger.info("Creator verification completed: memberId=$memberId")

        return CreatorVerificationResult(
            verified = true,
            role = Role.CREATOR,
            expiresAt = expiresAt,
            message = "창작자 인증이 완료되었습니다.",
        )
    }

    private data class ValidationData(
        val member: com.tropig.backend.member.entity.Member,
        val authInfo: com.tropig.backend.member.entity.MemberAuthInfo,
        val existingAccount: MemberAccount?,
    )

    fun getVerificationStatus(memberId: Long): CreatorVerificationStatusResponse {
        val member = memberRepository.findById(memberId).orElseThrow {
            MemberException("회원을 찾을 수 없습니다.", MessageCode.MEMBER_NOT_FOUND)
        }

        val account = memberAccountRepository.findByMemberId(memberId)

        return if (account != null) {
            val daysUntilExpiry = account.getDaysUntilExpiry().toInt()
            val expired = account.isExpired()

            CreatorVerificationStatusResponse(
                verified = true,
                role = member.role,
                expiresAt = account.expiresAt,
                daysUntilExpiry = daysUntilExpiry,
                expired = expired,
                canRenew = account.canRenew(),
                accountInfo = MaskedAccountInfo(
                    bankName = account.bankName,
                    accountNumber = maskAccountNumber(
                        encryptionService.decrypt(account.accountNumberEncrypted),
                    ),
                    accountHolder = maskAccountHolder(account.accountHolder),
                ),
                lastChangedAt = account.lastChangedAt,
                canChangeAccount = account.canChangeAccount(),
                nextChangeAvailableAt = account.getNextChangeAvailableAt(),
            )
        } else {
            CreatorVerificationStatusResponse(
                verified = false,
                role = member.role,
                expiresAt = null,
                daysUntilExpiry = null,
                expired = false,
                canRenew = false,
                accountInfo = null,
                lastChangedAt = null,
                canChangeAccount = false,
                nextChangeAvailableAt = null,
            )
        }
    }

    @Transactional
    fun renewVerification(memberId: Long): RenewVerificationResult {
        val account = memberAccountRepository.findByMemberId(memberId)
            ?: throw MemberException(
                "창작자 인증 정보가 없습니다.",
                MessageCode.CREATOR_VERIFICATION_NOT_FOUND,
            )

        if (!account.canRenew()) {
            val daysUntilExpiry = account.getDaysUntilExpiry()
            throw MemberException(
                "만료 30일 전부터 갱신 가능합니다. (현재 ${daysUntilExpiry}일 남음)",
                MessageCode.RENEWAL_TOO_EARLY,
            )
        }

        account.renew()
        memberAccountRepository.save(account)

        logger.info("Creator verification renewed: memberId=$memberId, newExpiresAt=${account.expiresAt}")

        return RenewVerificationResult(
            renewed = true,
            expiresAt = account.expiresAt,
            message = "창작자 인증이 갱신되었습니다.",
        )
    }

    @Transactional
    fun changeAccount(memberId: Long, request: ChangeAccountRequest): ChangeAccountResult {
        val account = memberAccountRepository.findByMemberId(memberId)
            ?: throw MemberException(
                "창작자 인증 정보가 없습니다.",
                MessageCode.CREATOR_VERIFICATION_NOT_FOUND,
            )

        if (!account.canChangeAccount()) {
            throw MemberException(
                "계좌 변경 후 30일간 재변경이 불가능합니다.",
                MessageCode.ACCOUNT_CHANGE_LOCKED,
            )
        }

        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)!!
        if (request.accountHolder != authInfo.name) {
            throw MemberException(
                "예금주명이 본인인증 실명과 일치하지 않습니다.",
                MessageCode.ACCOUNT_HOLDER_MISMATCH,
            )
        }

        val decryptedAccountNumber = encryptionService.decrypt(account.accountNumberEncrypted)
        if (decryptedAccountNumber == request.accountNumber && account.bankName == request.bankName) {
            throw MemberException(
                "기존 계좌와 동일합니다.",
                MessageCode.ACCOUNT_UNCHANGED,
            )
        }

        val verificationResult = bankAccountVerificationService.verifyBankAccount(
            bankName = request.bankName,
            accountNumber = request.accountNumber,
            expectedHolder = request.accountHolder,
        )

        if (!verificationResult.verified) {
            throw MemberException(
                verificationResult.message,
                MessageCode.ACCOUNT_HOLDER_MISMATCH,
            )
        }

        val encryptedAccountNumber = encryptionService.encrypt(request.accountNumber)
        account.updateAccount(
            bankName = request.bankName,
            accountNumberEncrypted = encryptedAccountNumber,
            accountHolder = request.accountHolder,
        )
        memberAccountRepository.save(account)

        val lockedUntil = account.lastChangedAt.plusDays(MemberAccount.ACCOUNT_CHANGE_LOCKOUT_DAYS)

        logger.info("Account changed: memberId=$memberId, newBank=${request.bankName}")

        return ChangeAccountResult(
            updated = true,
            message = "계좌 정보가 변경되었습니다. 30일간 재변경이 불가능합니다.",
            lockedUntil = lockedUntil,
            newAccountInfo = MaskedAccountInfo(
                bankName = request.bankName,
                accountNumber = maskAccountNumber(request.accountNumber),
                accountHolder = maskAccountHolder(request.accountHolder),
            ),
        )
    }

    private fun maskAccountNumber(accountNumber: String): String = if (accountNumber.length >= 6) {
        "${accountNumber.substring(0, 3)}-****-*${accountNumber.substring(accountNumber.length - 3)}"
    } else {
        "****"
    }

    private fun maskAccountHolder(name: String): String = if (name.length >= 3) {
        "${name.first()}${"*".repeat(name.length - 2)}${name.last()}"
    } else if (name.length == 2) {
        "${name.first()}*"
    } else {
        name
    }
}
