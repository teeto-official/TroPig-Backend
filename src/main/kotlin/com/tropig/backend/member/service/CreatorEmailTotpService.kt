package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MailType
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.common.util.EmailUtil
import com.tropig.backend.member.entity.Member
import com.tropig.backend.member.entity.MemberAuthInfo
import com.tropig.backend.member.enums.Role
import com.tropig.backend.member.model.request.CreatorEmailTotpConfirmRequest
import com.tropig.backend.member.model.response.CreatorEmailTotpConfirmResult
import com.tropig.backend.member.model.response.CreatorEmailTotpRequestResult
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.UUID

@Service
class CreatorEmailTotpService(
    private val memberRepository: MemberRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val sessionManager: CreatorEmailTotpSessionManager,
    private val emailUtil: EmailUtil,
) {
    private val logger = LoggerFactory.getLogger(CreatorEmailTotpService::class.java)
    private val creatorTotpTemplate = ClassPathResource(CREATOR_TOTP_TEMPLATE_PATH).inputStream.use {
        String(it.readAllBytes(), StandardCharsets.UTF_8)
    }

    fun sendTotp(memberId: Long): CreatorEmailTotpRequestResult {
        val member = findMember(memberId)
        val existingAuthInfo = memberAuthInfoRepository.findByMemberId(memberId)

        if (member.role == Role.CREATOR || existingAuthInfo?.creator == true) {
            throw MemberException("이미 창작자 인증이 완료되었습니다.", MessageCode.CREATOR_ALREADY_VERIFIED)
        }

        if (sessionManager.hasCooldown(memberId)) {
            throw MemberException("인증번호는 1분 후 다시 발송할 수 있습니다.", MessageCode.RESEND_LIMIT_EXCEEDED)
        }

        val session = sessionManager.createSession(memberId, member.email)
        try {
            emailUtil.send(
                to = member.email,
                mailType = MailType.CREATOR_TOTP,
                html = buildTotpHtml(session.code ?: error("TOTP code was not generated")),
            )
        } catch (e: Exception) {
            sessionManager.deleteSession(session.verificationId)
            sessionManager.clearCooldown(memberId)
            logger.error("Failed to send creator email TOTP: memberId=$memberId", e)
            throw MemberException(
                "인증번호 메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                MessageCode.EXTERNAL_SERVICE_ERROR,
            )
        }

        logger.info("Creator email TOTP sent: memberId=$memberId, email=${member.email}")

        return CreatorEmailTotpRequestResult(
            verificationId = session.verificationId,
            email = member.email,
            expiresAt = session.expiresAt,
            retryAvailableAt = session.retryAvailableAt,
            message = "인증번호가 이메일로 발송되었습니다. 10분 이내에 입력해주세요.",
        )
    }

    @Transactional
    fun confirmTotp(memberId: Long, request: CreatorEmailTotpConfirmRequest): CreatorEmailTotpConfirmResult {
        val session = sessionManager.getSession(request.verificationId)
            ?: throw MemberException("인증 요청을 찾을 수 없습니다. 다시 시도해주세요.", MessageCode.VERIFICATION_NOT_FOUND)

        if (session.memberId != memberId) {
            throw MemberException("잘못된 인증 요청입니다.", MessageCode.VERIFICATION_NOT_FOUND)
        }

        if (sessionManager.isExpired(session)) {
            sessionManager.deleteSession(request.verificationId)
            throw MemberException("인증번호가 만료되었습니다. 다시 요청해주세요.", MessageCode.OTP_EXPIRED)
        }

        if (MemberAuthInfo.sha256(request.totp) != session.codeHash) {
            handleInvalidTotp(request.verificationId, session)
        }

        val member = findMember(memberId)
        if (member.email != session.email) {
            throw MemberException("인증 요청의 이메일과 현재 회원 이메일이 일치하지 않습니다.", MessageCode.INVALID_PARAMS)
        }

        val now = LocalDateTime.now()
        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)?.apply {
            creator = true
            authCreatorAt = now
        } ?: createEmailAuthInfo(member, now)

        memberAuthInfoRepository.save(authInfo)

        member.role = Role.CREATOR
        memberRepository.save(member)

        sessionManager.deleteSession(request.verificationId)
        sessionManager.clearCooldown(memberId)

        logger.info("Creator email TOTP verified: memberId=$memberId")

        return CreatorEmailTotpConfirmResult(
            verified = true,
            role = Role.CREATOR,
            verifiedAt = now,
            message = "이메일 인증이 완료되어 창작자 권한이 부여되었습니다.",
        )
    }

    private fun handleInvalidTotp(verificationId: String, session: CreatorEmailTotpSession): Nothing {
        try {
            sessionManager.incrementAttempts(verificationId)
            val remaining = sessionManager.getRemainingAttempts(session) - 1
            throw MemberException(
                "인증번호가 일치하지 않습니다. (남은 시도: ${remaining}회)",
                MessageCode.INVALID_OTP,
            )
        } catch (e: OtpAttemptsExceededException) {
            throw MemberException(
                "인증번호 입력 횟수를 초과했습니다. 처음부터 다시 시도해주세요.",
                MessageCode.OTP_ATTEMPTS_EXCEEDED,
            )
        }
    }

    private fun createEmailAuthInfo(member: Member, now: LocalDateTime): MemberAuthInfo = MemberAuthInfo(
        memberId = member.id,
        ci = UUID.nameUUIDFromBytes(member.nickname.toByteArray()).toString(),
        di = UUID.nameUUIDFromBytes(member.nickname.toByteArray()).toString(),
        name = member.nickname.take(20).ifBlank { "EMAIL_VERIFIED" },
        birthDate = "1900-01-01",
        phoneNumber = "00000000000",
        verifiedAt = now,
        creator = true,
        authCreatorAt = now,
    )

    private fun findMember(memberId: Long): Member = memberRepository.findById(memberId).orElseThrow {
        MemberException("회원을 찾을 수 없습니다.", MessageCode.MEMBER_NOT_FOUND)
    }

    private fun buildTotpHtml(code: String): String = creatorTotpTemplate.replace(TOTP_CODE_PLACEHOLDER, code)

    companion object {
        private const val CREATOR_TOTP_TEMPLATE_PATH = "mail/creator-totp.html"
        private const val TOTP_CODE_PLACEHOLDER = "{{code}}"
    }
}
