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
import com.tropig.backend.partner.client.BankAccount
import com.tropig.backend.partner.client.ContactInfo
import com.tropig.backend.partner.client.CreatePartnerRequest
import com.tropig.backend.partner.client.PartnerType
import com.tropig.backend.partner.client.PortOnePartnerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 창작자 인증 서비스
 * 본인인증 후 은행 계좌 등록 및 PortOne 파트너 등록
 */
@Service
class CreatorVerificationService(
    private val memberRepository: MemberRepository,
    private val memberAccountRepository: MemberAccountRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val bankAccountVerificationService: BankAccountVerificationService,
    private val portOnePartnerClient: PortOnePartnerClient,
    private val encryptionService: EncryptionService,
) {
    private val logger = LoggerFactory.getLogger(CreatorVerificationService::class.java)

    /**
     * 창작자 인증 (계좌 등록 및 파트너 생성)
     *
     * Transaction management strategy:
     * 1. Validate all prerequisites (no transaction)
     * 2. Register PortOne partner (external API, outside transaction)
     * 3. Save to database (transactional)
     * 4. If database save fails, log for manual cleanup
     */
    fun verifyCreator(memberId: Long, request: CreatorVerificationRequest): CreatorVerificationResult {
        // 1. 모든 사전 조건 검증 (트랜잭션 외부)
        val validationData = validateCreatorVerification(memberId, request)

        // 2. PortOne Partner 등록 (트랜잭션 외부)
        val partnerId = try {
            registerPartner(
                memberId = memberId,
                name = validationData.authInfo.name,
                email = validationData.member.email,
                bankName = request.bankName,
                accountNumber = request.accountNumber,
                accountHolder = request.accountHolder,
            )
        } catch (e: Exception) {
            logger.error("Failed to register PortOne partner for memberId=$memberId", e)
            throw MemberException(
                "파트너 등록에 실패했습니다.",
                MessageCode.PORTONE_PARTNER_CREATE_FAILED,
            )
        }

        // 3. 데이터베이스 저장 (트랜잭션)
        return try {
            saveCreatorVerification(
                memberId = memberId,
                request = request,
                partnerId = partnerId,
                existingAccount = validationData.existingAccount,
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to save creator verification for memberId=$memberId, partnerId=$partnerId. " +
                    "Manual cleanup may be required for PortOne partner.",
                e,
            )
            throw e
        }
    }

    /**
     * 창작자 인증 사전 조건 검증
     */
    private fun validateCreatorVerification(memberId: Long, request: CreatorVerificationRequest): ValidationData {
        // 1. 본인인증 완료 여부 확인
        val member = memberRepository.findById(memberId).orElseThrow {
            MemberException("회원을 찾을 수 없습니다.", MessageCode.MEMBER_NOT_FOUND)
        }

        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)
            ?: throw MemberException(
                "본인인증이 필요합니다.",
                MessageCode.IDENTITY_VERIFICATION_REQUIRED,
            )

        // 2. 이미 창작자 인증이 완료되었는지 확인
        val existingAccount = memberAccountRepository.findByMemberId(memberId)
        if (existingAccount != null && !existingAccount.isExpired()) {
            throw MemberException(
                "이미 창작자 인증이 완료되었습니다.",
                MessageCode.CREATOR_ALREADY_VERIFIED,
            )
        }

        // 3. 예금주명과 본인인증 실명 일치 확인
        if (request.accountHolder != authInfo.name) {
            throw MemberException(
                "예금주명이 본인인증 실명과 일치하지 않습니다.",
                MessageCode.ACCOUNT_HOLDER_MISMATCH,
            )
        }

        // 4. 은행 계좌 실명 인증 (PortOne Bank Account API)
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

    /**
     * 창작자 인증 정보 저장 (트랜잭션)
     */
    @Transactional
    private fun saveCreatorVerification(
        memberId: Long,
        request: CreatorVerificationRequest,
        partnerId: String,
        existingAccount: MemberAccount?,
    ): CreatorVerificationResult {
        val member = memberRepository.findById(memberId).orElseThrow()

        // 1. MemberAccount 저장 (계좌번호 암호화)
        val now = LocalDateTime.now()
        val expiresAt = now.plusYears(MemberAccount.VERIFICATION_VALIDITY_YEARS)
        val encryptedAccountNumber = encryptionService.encrypt(request.accountNumber)

        val memberAccount = if (existingAccount != null) {
            // 만료된 계좌가 있으면 갱신
            existingAccount.apply {
                this.bankName = request.bankName
                this.accountNumberEncrypted = encryptedAccountNumber
                this.accountHolder = request.accountHolder
                this.verifiedAt = now
                this.expiresAt = expiresAt
                this.lastChangedAt = now
            }
        } else {
            // 새로 생성
            MemberAccount(
                memberId = memberId,
                bankName = request.bankName,
                accountNumberEncrypted = encryptedAccountNumber,
                accountHolder = request.accountHolder,
                portonePartnerId = partnerId,
                verifiedAt = now,
                expiresAt = expiresAt,
                lastChangedAt = now,
            )
        }
        memberAccountRepository.save(memberAccount)

        // 2. Member role → CREATOR 업데이트
        member.role = Role.CREATOR
        memberRepository.save(member)

        logger.info("Creator verification completed: memberId=$memberId, partnerId=$partnerId")

        return CreatorVerificationResult(
            verified = true,
            role = Role.CREATOR,
            expiresAt = expiresAt,
            message = "창작자 인증이 완료되었습니다.",
            partnerId = partnerId,
        )
    }

    /**
     * 검증 데이터 홀더
     */
    private data class ValidationData(
        val member: com.tropig.backend.member.entity.Member,
        val authInfo: com.tropig.backend.member.entity.MemberAuthInfo,
        val existingAccount: MemberAccount?,
    )

    /**
     * 창작자 인증 상태 조회
     */
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
                partnerId = account.portonePartnerId,
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
                partnerId = null,
                lastChangedAt = null,
                canChangeAccount = false,
                nextChangeAvailableAt = null,
            )
        }
    }

    /**
     * 창작자 인증 갱신 (기존 계좌로 1년 연장)
     */
    @Transactional
    fun renewVerification(memberId: Long): RenewVerificationResult {
        val account = memberAccountRepository.findByMemberId(memberId)
            ?: throw MemberException(
                "창작자 인증 정보가 없습니다.",
                MessageCode.CREATOR_VERIFICATION_NOT_FOUND,
            )

        // 만료 30일 전부터 갱신 가능
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

    /**
     * 계좌 정보 변경
     */
    @Transactional
    fun changeAccount(memberId: Long, request: ChangeAccountRequest): ChangeAccountResult {
        val account = memberAccountRepository.findByMemberId(memberId)
            ?: throw MemberException(
                "창작자 인증 정보가 없습니다.",
                MessageCode.CREATOR_VERIFICATION_NOT_FOUND,
            )

        // 30일 이내 재변경 불가 체크
        if (!account.canChangeAccount()) {
            val nextChangeAt = account.getNextChangeAvailableAt()
            throw MemberException(
                "계좌 변경 후 30일간 재변경이 불가능합니다.",
                MessageCode.ACCOUNT_CHANGE_LOCKED,
            )
        }

        // 본인인증 정보 확인
        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)!!
        if (request.accountHolder != authInfo.name) {
            throw MemberException(
                "예금주명이 본인인증 실명과 일치하지 않습니다.",
                MessageCode.ACCOUNT_HOLDER_MISMATCH,
            )
        }

        // 기존 계좌와 동일한지 확인
        val decryptedAccountNumber = encryptionService.decrypt(account.accountNumberEncrypted)
        if (decryptedAccountNumber == request.accountNumber && account.bankName == request.bankName) {
            throw MemberException(
                "기존 계좌와 동일합니다.",
                MessageCode.ACCOUNT_UNCHANGED,
            )
        }

        // 은행 계좌 실명 인증
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

        // PortOne Partner 계좌 정보 업데이트
        updatePartnerAccount(
            partnerId = account.portonePartnerId,
            bankName = request.bankName,
            accountNumber = request.accountNumber,
            accountHolder = request.accountHolder,
        )

        // 계좌 정보 업데이트
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

    /**
     * PortOne 파트너 등록
     */
    private fun registerPartner(
        memberId: Long,
        name: String,
        email: String,
        bankName: String,
        accountNumber: String,
        accountHolder: String,
    ): String {
        val partnerRequest = CreatePartnerRequest(
            id = "member_$memberId",
            name = name,
            contact = ContactInfo(email = email),
            account = BankAccount(
                bank = com.tropig.backend.member.enums.BankCode.toPortOneCode(bankName),
                accountNumber = accountNumber,
                holder = accountHolder,
            ),
            type = PartnerType.NON_WHT_PAYER, // 기본: 개인 (원천징수 미대상자)
        )

        val response = portOnePartnerClient.createPartner(partnerRequest)
        return response.id
    }

    /**
     * PortOne 파트너 계좌 정보 업데이트
     */
    private fun updatePartnerAccount(
        partnerId: String,
        bankName: String,
        accountNumber: String,
        accountHolder: String,
    ) {
        val account = BankAccount(
            bank = com.tropig.backend.member.enums.BankCode.toPortOneCode(bankName),
            accountNumber = accountNumber,
            holder = accountHolder,
        )
        portOnePartnerClient.updatePartnerAccount(partnerId, account)
    }

    /**
     * 계좌번호 마스킹 (예: "1234567890" → "123-****-*890")
     */
    private fun maskAccountNumber(accountNumber: String): String = if (accountNumber.length >= 6) {
        "${accountNumber.substring(0, 3)}-****-*${accountNumber.substring(accountNumber.length - 3)}"
    } else {
        "****"
    }

    /**
     * 예금주명 마스킹 (예: "홍길동" → "홍*동")
     */
    private fun maskAccountHolder(name: String): String = if (name.length >= 3) {
        "${name.first()}${"*".repeat(name.length - 2)}${name.last()}"
    } else if (name.length == 2) {
        "${name.first()}*"
    } else {
        name
    }
}
