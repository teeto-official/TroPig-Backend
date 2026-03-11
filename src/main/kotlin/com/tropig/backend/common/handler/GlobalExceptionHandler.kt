package com.tropig.backend.common.handler

import com.tropig.backend.common.exception.*
import com.tropig.backend.common.model.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): ResponseEntity<ErrorResponse> {
        logger.debug("NotFoundException: ${e.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.debug("IllegalArgumentException: ${e.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(MemberException::class)
    fun handleMemberException(e: MemberException): ResponseEntity<ErrorResponse> {
        logger.debug("MemberException: ${e.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(PaymentException::class)
    fun handlePaymentException(e: PaymentException): ResponseEntity<ErrorResponse> {
        logger.debug("PaymentException: ${e.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(ContentException::class)
    fun handleContentException(e: ContentException): ResponseEntity<ErrorResponse> {
        logger.debug("ContentException: ${e.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
        logger.debug("ResponseStatusException: ${e.reason}")
        return ResponseEntity.status(e.statusCode)
            .body(ErrorResponse(message = e.reason ?: "요청 처리 중 오류가 발생했습니다."))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(message = "서버 내부 오류가 발생했습니다."))
    }
}
