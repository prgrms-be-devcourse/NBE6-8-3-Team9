package com.back.back9.global.exception

import com.back.back9.global.error.ErrorCode
import com.back.back9.global.error.ErrorException
import com.back.back9.global.problem.ProblemDetails
import com.back.back9.global.rsData.RsData
import jakarta.validation.ConstraintViolationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    // 비즈니스 도메인 에러 (ErrorException)
    @ExceptionHandler(ErrorException::class)
    fun handleErrorException(ex: ErrorException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetails.of(ex.errorCode, *ex.args)
        return ResponseEntity.status(ex.errorCode.status).body(pd)
    }

    // 서비스 계층 에러 (RsData 포맷)
    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(ex: ServiceException): ResponseEntity<RsData<Void>> {
        return ResponseEntity.status(ex.rsData.statusCode).body(ex.rsData)
    }

    // @Valid 바인딩 에러
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetails.of(ErrorCode.INVALID_REQUEST)
        pd.setProperty(
            "fieldErrors",
            ex.bindingResult.fieldErrors.map {
                mapOf(
                    "field" to it.field,
                    "rejectedValue" to it.rejectedValue,
                    "message" to it.defaultMessage
                )
            }
        )
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status).body(pd)
    }

    // 바인딩/제약조건/메시지 파싱 에러
    @ExceptionHandler(
        value = [
            BindException::class,
            ConstraintViolationException::class,
            HttpMessageNotReadableException::class
        ]
    )
    fun handleBindException(ex: Exception): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetails.of(ErrorCode.INVALID_REQUEST, ex.message)
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status).body(pd)
    }

    // 경로 파라미터 타입 오류를 400으로 반환
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(@Suppress("UNUSED_PARAMETER") ex: MethodArgumentTypeMismatchException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("잘못된 경로 파라미터 형식입니다.")
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ProblemDetail> {
        log.error("Unhandled exception occurred: {}", ex.message, ex)
        val pd = ProblemDetails.of(ErrorCode.INTERNAL_ERROR, ex.message)
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status).body(pd)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetails.of(ErrorCode.INVALID_REQUEST, ex.message)
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status).body(pd)
    }
}
