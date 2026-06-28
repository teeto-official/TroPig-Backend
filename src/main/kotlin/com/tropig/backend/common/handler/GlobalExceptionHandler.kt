package com.tropig.backend.common.handler

import com.tropig.backend.common.exception.*
import com.tropig.backend.common.model.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun errorResponse(status: HttpStatusCode, response: ErrorResponse): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): ResponseEntity<ErrorResponse> {
        logger.debug("NotFoundException: ${e.message}")
        return errorResponse(HttpStatus.NOT_FOUND, ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.debug("IllegalArgumentException: ${e.message}")
        return errorResponse(HttpStatus.BAD_REQUEST, ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(MemberException::class)
    fun handleMemberException(e: MemberException): ResponseEntity<ErrorResponse> {
        logger.debug("MemberException: ${e.message}")
        return errorResponse(HttpStatus.BAD_REQUEST, ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(PaymentException::class)
    fun handlePaymentException(e: PaymentException): ResponseEntity<ErrorResponse> {
        logger.debug("PaymentException: ${e.message}")
        return errorResponse(HttpStatus.BAD_REQUEST, ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(ContentException::class)
    fun handleContentException(e: ContentException): ResponseEntity<ErrorResponse> {
        logger.debug("ContentException: ${e.message}")
        return errorResponse(HttpStatus.BAD_REQUEST, ErrorResponse(message = e.message!!, code = e.code))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
        logger.debug("ResponseStatusException: ${e.reason}")
        return errorResponse(e.statusCode, ErrorResponse(message = e.reason ?: "요청 처리 중 오류가 발생했습니다."))
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(e: NoHandlerFoundException): ResponseEntity<ErrorResponse> {
        logger.debug("NoHandlerFoundException: ${e.message}")
        return errorResponse(HttpStatus.NOT_FOUND, ErrorResponse(message = "요청한 경로를 찾을 수 없습니다."))
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleMediaTypeNotAcceptable(e: HttpMediaTypeNotAcceptableException): ResponseEntity<ErrorResponse> {
        logger.debug("HttpMediaTypeNotAcceptableException: ${e.message}")
        return errorResponse(HttpStatus.NOT_ACCEPTABLE, ErrorResponse(message = "Not Acceptable"))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResponse(message = "서버 내부 오류가 발생했습니다."))
    }
}
