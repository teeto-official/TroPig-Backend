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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    private fun buildTotpHtml(code: String): String {
        val displayCode = code

        return """
            <!doctype html>
            <html lang="ko">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>TEETO 창작자 이메일 인증</title>
              </head>
              <body style="margin: 0; padding: 0; background-color: #f4f6f8;">
                <div style="display: none; max-height: 0; overflow: hidden; opacity: 0;">
                  TEETO 창작자 인증번호 $displayCode 를 10분 이내에 입력해주세요.
                </div>
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0"
                    style="width: 100%; border-collapse: collapse; background-color: #f4f6f8;">
                  <tr>
                    <td align="center" style="padding: 32px 16px;">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0"
                          style="width: 100%; max-width: 560px; border-collapse: collapse;">
                        <tr>
                          <td style="padding: 0 0 14px 0; font-family: Arial, sans-serif;">
                            <div style="font-size: 20px; font-weight: 800; color: #111827;">TEETO</div>
                            <div style="margin-top: 4px; font-size: 13px; color: #6b7280;">Creator Verification</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px;
                              overflow: hidden; font-family: Arial, sans-serif;">
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0"
                                style="width: 100%; border-collapse: collapse;">
                              <tr>
                                <td style="padding: 32px 28px 24px 28px;">
                                  <h1 style="margin: 0; font-size: 24px; line-height: 1.35; color: #111827;
                                      font-weight: 800;">
                                    창작자 이메일 인증번호
                                  </h1>
                                  <p style="margin: 14px 0 0 0; font-size: 15px; line-height: 1.7; color: #374151;">
                                    TEETO 창작자 인증을 완료하려면 아래 인증번호를 입력해주세요.
                                  </p>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding: 0 28px 24px 28px;">
                                  <div style="background-color: #f9fafb; border: 1px solid #d1d5db;
                                      border-radius: 8px; padding: 22px 18px; text-align: center;">
                                    <div style="font-size: 12px; font-weight: 700; color: #6b7280;
                                        letter-spacing: 1.2px;">
                                      VERIFICATION CODE
                                    </div>
                                    <div style="margin-top: 8px; font-size: 36px; line-height: 1.2; font-weight: 800;
                                        color: #111827; letter-spacing: 6px;">
                                      $displayCode
                                    </div>
                                  </div>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding: 0 28px 30px 28px;">
                                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0"
                                      style="width: 100%; border-collapse: collapse; background-color: #fff7ed;
                                      border: 1px solid #fed7aa; border-radius: 8px;">
                                    <tr>
                                      <td style="padding: 14px 16px; font-family: Arial, sans-serif;">
                                        <p style="margin: 0; font-size: 14px; line-height: 1.6; color: #9a3412;">
                                          인증번호는 10분 동안 유효하며, 타인에게 공유하지 마세요.
                                        </p>
                                      </td>
                                    </tr>
                                  </table>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding: 18px 4px 0 4px; font-family: Arial, sans-serif;">
                            <p style="margin: 0; font-size: 12px; line-height: 1.6; color: #9ca3af;">
                              본인이 요청하지 않은 메일이라면 이 메시지를 무시해주세요.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()
    }
}
