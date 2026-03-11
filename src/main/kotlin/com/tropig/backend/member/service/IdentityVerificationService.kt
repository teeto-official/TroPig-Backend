package com.tropig.backend.member.service

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.MemberException
import com.tropig.backend.member.client.CustomerInfo
import com.tropig.backend.member.client.PortOneIdentityVerificationClient
import com.tropig.backend.member.client.PortOneIdentityVerificationException
import com.tropig.backend.member.client.SendVerificationRequest
import com.tropig.backend.member.entity.MemberAuthInfo
import com.tropig.backend.member.model.request.IdentityVerificationCompleteDto
import com.tropig.backend.member.model.request.VerificationConfirmDto
import com.tropig.backend.member.model.request.VerificationRequestDto
import com.tropig.backend.member.model.request.VerificationResendDto
import com.tropig.backend.member.model.response.*
import com.tropig.backend.member.repository.MemberAuthInfoRepository
import com.tropig.backend.member.repository.MemberRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 본인인증 서비스
 * PortOne Identity Verification API를 사용한 본인인증 처리
 */
@Service
class IdentityVerificationService(
    private val memberRepository: MemberRepository,
    private val memberAuthInfoRepository: MemberAuthInfoRepository,
    private val portOneClient: PortOneIdentityVerificationClient,
    private val sessionManager: VerificationSessionManager,
    private val httpRequest: HttpServletRequest,
) {
    private val logger = LoggerFactory.getLogger(IdentityVerificationService::class.java)

    /**
     * 본인인증 요청을 시작합니다.
     * OTP를 SMS로 전송합니다.
     */
    fun requestVerification(memberId: Long, request: VerificationRequestDto): VerificationRequestResult {
        logger.info("Starting identity verification for member: $memberId")

        // 1. 이미 인증된 사용자 체크
        if (memberAuthInfoRepository.existsByMemberId(memberId)) {
            throw MemberException("이미 본인인증이 완료되었습니다.", MessageCode.ALREADY_VERIFIED)
        }

        // 2. 사용자 IP 주소 가져오기
        val ipAddress = getClientIpAddress()

        // 3. PortOne API 호출
        val portoneId = "portone_identity_${System.currentTimeMillis()}_$memberId"
        val portoneRequest = SendVerificationRequest(
            identityVerificationId = portoneId,
            customer = CustomerInfo(
                name = request.name,
                phoneNumber = request.phoneNumber,
                ipAddress = ipAddress,
                identityNumber = request.idNumber,
            ),
            operator = request.carrier.name,
            method = request.method.name,
            customData = "tropig_member_id:$memberId",
        )

        return try {
            val response = portOneClient.sendVerification(portoneRequest)

            // 4. 세션 생성 (Redis)
            val session = sessionManager.createSession(memberId, response.id)

            logger.info("Verification request sent successfully: sessionId=${session.sessionId}")

            VerificationRequestResult(
                verificationId = session.sessionId,
                expiresAt = session.expiresAt,
                method = request.method,
                message = "인증번호가 발송되었습니다. 3분 이내에 입력해주세요.",
            )
        } catch (e: PortOneIdentityVerificationException) {
            logger.error("Failed to request verification from PortOne", e)
            throw MemberException(
                "본인인증 요청 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                MessageCode.EXTERNAL_SERVICE_ERROR,
            )
        }
    }

    /**
     * OTP 코드로 본인인증을 확인합니다.
     */
    @Transactional
    fun confirmVerification(memberId: Long, request: VerificationConfirmDto): VerificationResult {
        logger.info("Confirming identity verification: member=$memberId, sessionId=${request.verificationId}")

        // 1. 세션 조회
        val session = sessionManager.getSession(request.verificationId)
            ?: throw MemberException("인증 요청을 찾을 수 없습니다. 다시 시도해주세요.", MessageCode.VERIFICATION_NOT_FOUND)

        // 2. 세션 소유자 확인
        if (session.memberId != memberId) {
            throw MemberException("잘못된 인증 요청입니다.", MessageCode.VERIFICATION_NOT_FOUND)
        }

        // 3. 만료 확인
        if (sessionManager.isExpired(session)) {
            sessionManager.deleteSession(request.verificationId)
            throw MemberException("인증번호가 만료되었습니다. 다시 요청해주세요.", MessageCode.OTP_EXPIRED)
        }

        // 4. PortOne API 확인 호출
        return try {
            val confirmResponse = portOneClient.confirmVerification(session.portoneId, request.otp)

            // 5. CI/DI 중복 확인
            val verifiedCustomer = confirmResponse.verifiedCustomer
            verifiedCustomer.ci?.let { ci ->
                if (memberAuthInfoRepository.existsByCi(ci)) {
                    throw MemberException(
                        "이미 다른 계정에서 본인인증이 완료된 정보입니다.",
                        MessageCode.CI_ALREADY_EXISTS,
                    )
                }
            }

            verifiedCustomer.di?.let { di ->
                if (memberAuthInfoRepository.existsByDi(di)) {
                    throw MemberException(
                        "이미 등록된 본인인증 정보입니다.",
                        MessageCode.DI_ALREADY_EXISTS,
                    )
                }
            }

            // 6. 나이 계산
            val isAdult = MemberAuthInfo.isAdult(verifiedCustomer.birthDate)

            // 7. MemberAuthInfo 저장
            val authInfo = MemberAuthInfo(
                memberId = memberId,
                name = verifiedCustomer.name,
                birthDate = verifiedCustomer.birthDate,
                phoneNumber = verifiedCustomer.phoneNumber,
                ci = verifiedCustomer.ci,
                di = verifiedCustomer.di,
                verifiedAt = LocalDateTime.now(),
            )
            memberAuthInfoRepository.save(authInfo)

            // 8. Member의 adult 플래그 업데이트
            val member = memberRepository.findById(memberId)
                .orElseThrow { MemberException("회원을 찾을 수 없습니다.", MessageCode.NOT_FOUND_MEMBER) }
            member.adult = isAdult
            memberRepository.save(member)

            // 9. 세션 삭제
            sessionManager.deleteSession(request.verificationId)

            logger.info("Identity verification completed successfully: member=$memberId, adult=$isAdult")

            VerificationResult(
                verified = true,
                adult = isAdult,
                name = verifiedCustomer.name,
                birthDate = verifiedCustomer.birthDate,
                phoneNumber = MemberAuthInfo.maskPhoneNumber(verifiedCustomer.phoneNumber),
                verifiedAt = LocalDateTime.now(),
                message = "본인인증이 완료되었습니다.",
            )
        } catch (e: PortOneIdentityVerificationException) {
            // OTP 시도 횟수 증가
            try {
                val attempts = sessionManager.incrementOtpAttempts(request.verificationId)
                val remaining = sessionManager.getRemainingAttempts(session)

                logger.warn("Invalid OTP attempt: member=$memberId, attempts=$attempts, remaining=$remaining")

                throw MemberException(
                    "인증번호가 일치하지 않습니다. (남은 시도: ${remaining}회)",
                    MessageCode.INVALID_OTP,
                )
            } catch (e: OtpAttemptsExceededException) {
                logger.warn("OTP attempts exceeded: member=$memberId")
                throw MemberException(
                    "인증번호 입력 횟수를 초과했습니다. 처음부터 다시 시도해주세요.",
                    MessageCode.OTP_ATTEMPTS_EXCEEDED,
                )
            }
        }
    }

    /**
     * PortOne SDK 방식 본인인증을 완료합니다.
     * 프론트엔드에서 PortOne SDK로 인증 완료 후 identityVerificationId를 전달받아 처리합니다.
     */
    @Transactional
    fun completeVerification(memberId: Long, request: IdentityVerificationCompleteDto): VerificationResult {
        logger.info("Completing identity verification (SDK): member=$memberId, id=${request.identityVerificationId}")

        // 1. 이미 인증된 사용자 체크
        if (memberAuthInfoRepository.existsByMemberId(memberId)) {
            throw MemberException("이미 본인인증이 완료되었습니다.", MessageCode.ALREADY_VERIFIED)
        }

        // 2. PortOne API에서 인증 결과 조회
        return try {
            val response = portOneClient.getVerification(request.identityVerificationId)
            val verifiedCustomer = response.verifiedCustomer

            // 3. CI/DI 중복 확인
            verifiedCustomer.ci?.let { ci ->
                if (memberAuthInfoRepository.existsByCi(ci)) {
                    throw MemberException(
                        "이미 다른 계정에서 본인인증이 완료된 정보입니다.",
                        MessageCode.CI_ALREADY_EXISTS,
                    )
                }
            }

            verifiedCustomer.di?.let { di ->
                if (memberAuthInfoRepository.existsByDi(di)) {
                    throw MemberException(
                        "이미 등록된 본인인증 정보입니다.",
                        MessageCode.DI_ALREADY_EXISTS,
                    )
                }
            }

            // 4. 나이 계산
            val isAdult = MemberAuthInfo.isAdult(verifiedCustomer.birthDate)

            // 5. MemberAuthInfo 저장
            val authInfo = MemberAuthInfo(
                memberId = memberId,
                name = verifiedCustomer.name,
                birthDate = verifiedCustomer.birthDate,
                phoneNumber = verifiedCustomer.phoneNumber,
                ci = verifiedCustomer.ci,
                di = verifiedCustomer.di,
                verifiedAt = LocalDateTime.now(),
            )
            memberAuthInfoRepository.save(authInfo)

            // 6. Member의 adult 플래그 업데이트
            val member = memberRepository.findById(memberId)
                .orElseThrow { MemberException("회원을 찾을 수 없습니다.", MessageCode.NOT_FOUND_MEMBER) }
            member.adult = isAdult
            memberRepository.save(member)

            logger.info("Identity verification (SDK) completed: member=$memberId, adult=$isAdult")

            VerificationResult(
                verified = true,
                adult = isAdult,
                name = verifiedCustomer.name,
                birthDate = verifiedCustomer.birthDate,
                phoneNumber = MemberAuthInfo.maskPhoneNumber(verifiedCustomer.phoneNumber),
                verifiedAt = LocalDateTime.now(),
                message = "본인인증이 완료되었습니다.",
            )
        } catch (e: MemberException) {
            throw e
        } catch (e: PortOneIdentityVerificationException) {
            logger.error("Failed to complete verification from PortOne", e)
            throw MemberException(
                "본인인증 확인 중 오류가 발생했습니다: ${e.message}",
                MessageCode.EXTERNAL_SERVICE_ERROR,
            )
        }
    }

    /**
     * OTP를 재전송합니다.
     */
    fun resendOtp(memberId: Long, request: VerificationResendDto): VerificationResendResult {
        logger.info("Resending OTP: member=$memberId, sessionId=${request.verificationId}")

        // 1. 세션 조회
        val session = sessionManager.getSession(request.verificationId)
            ?: throw MemberException("인증 요청을 찾을 수 없습니다.", MessageCode.VERIFICATION_NOT_FOUND)

        // 2. 세션 소유자 확인
        if (session.memberId != memberId) {
            throw MemberException("잘못된 인증 요청입니다.", MessageCode.VERIFICATION_NOT_FOUND)
        }

        // 3. 재전송 횟수 확인 및 증가
        return try {
            val newCount = sessionManager.incrementResendCount(request.verificationId)
            val remaining = sessionManager.getRemainingResends(session)

            // 4. PortOne API 재전송 호출
            portOneClient.resendVerification(session.portoneId)

            // 5. 갱신된 세션 조회
            val updatedSession = sessionManager.getSession(request.verificationId)!!

            logger.info("OTP resent successfully: member=$memberId, resendCount=$newCount")

            VerificationResendResult(
                sent = true,
                expiresAt = updatedSession.expiresAt,
                message = "인증번호가 재전송되었습니다.",
                remainingResends = remaining - 1,
            )
        } catch (e: ResendLimitExceededException) {
            logger.warn("Resend limit exceeded: member=$memberId")
            throw MemberException(
                "인증번호 재전송 횟수를 초과했습니다. 처음부터 다시 시도해주세요.",
                MessageCode.RESEND_LIMIT_EXCEEDED,
            )
        } catch (e: PortOneIdentityVerificationException) {
            logger.error("Failed to resend OTP", e)
            throw MemberException(
                "인증번호 재전송 중 오류가 발생했습니다.",
                MessageCode.EXTERNAL_SERVICE_ERROR,
            )
        }
    }

    /**
     * 본인인증 상태를 조회합니다.
     */
    fun getVerificationStatus(memberId: Long): VerificationStatusResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { MemberException("회원을 찾을 수 없습니다.", MessageCode.NOT_FOUND_MEMBER) }

        val authInfo = memberAuthInfoRepository.findByMemberId(memberId)

        return if (authInfo != null) {
            VerificationStatusResponse(
                verified = true,
                adult = member.adult,
                verifiedAt = authInfo.verifiedAt,
                name = authInfo.name,
                phoneNumber = authInfo.getMaskedPhoneNumber(),
                birthDate = authInfo.birthDate,
                age = authInfo.getAge(),
            )
        } else {
            VerificationStatusResponse(
                verified = false,
                adult = false,
                verifiedAt = null,
                name = null,
                phoneNumber = null,
                birthDate = null,
                age = null,
            )
        }
    }

    /**
     * 클라이언트 IP 주소를 가져옵니다.
     */
    private fun getClientIpAddress(): String {
        val xForwardedFor = httpRequest.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",")[0].trim()
        } else {
            httpRequest.remoteAddr
        }
    }
}
