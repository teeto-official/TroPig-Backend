package com.tropig.backend.common.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    data class ErrorResponse(
        val code: String,
        val message: String,
    )

    @ExceptionHandler(InvalidBankAccountException::class)
    fun handleInvalidAccount(e: InvalidBankAccountException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(
            ErrorResponse(
                code = "INVALID_BANK_ACCOUNT",
                message = e.message ?: "계좌 인증에 실패했습니다"
            )
        )

    @ExceptionHandler(ExternalVerificationFailedException::class)
    fun handleExternalError(e: ExternalVerificationFailedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(503).body(
            ErrorResponse(
                code = "BANK_VERIFICATION_UNAVAILABLE",
                message = "현재 계좌 인증이 불가능합니다. 잠시 후 다시 시도해주세요."
            )
        )
}