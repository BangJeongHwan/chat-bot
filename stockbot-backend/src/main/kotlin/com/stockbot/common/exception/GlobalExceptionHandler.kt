package com.stockbot.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(ex: EmailAlreadyExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                code = "EMAIL_ALREADY_EXISTS",
                message = "이미 사용 중인 이메일입니다."
            )
        )
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(
                code = "INVALID_CREDENTIALS",
                message = "이메일 또는 비밀번호가 올바르지 않습니다."
            )
        )
    }

    @ExceptionHandler(RefreshTokenInvalidException::class)
    fun handleRefreshTokenInvalid(ex: RefreshTokenInvalidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(
                code = "REFRESH_TOKEN_INVALID",
                message = "유효하지 않은 토큰입니다. 다시 로그인해주세요."
            )
        )
    }

    @ExceptionHandler(RefreshTokenExpiredException::class)
    fun handleRefreshTokenExpired(ex: RefreshTokenExpiredException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(
                code = "REFRESH_TOKEN_EXPIRED",
                message = "토큰이 만료되었습니다. 다시 로그인해주세요."
            )
        )
    }

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceeded(ex: RateLimitExceededException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
            ErrorResponse(
                code = "RATE_LIMIT_EXCEEDED",
                message = ex.message ?: "요청 한도를 초과했습니다."
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.associate { fieldError ->
            fieldError.field to (fieldError.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "VALIDATION_ERROR",
                message = "입력값이 올바르지 않습니다.",
                details = details
            )
        )
    }
}
