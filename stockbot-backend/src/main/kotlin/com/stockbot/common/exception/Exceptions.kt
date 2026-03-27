package com.stockbot.common.exception

class EmailAlreadyExistsException : RuntimeException()

class InvalidCredentialsException : RuntimeException()

class RefreshTokenInvalidException : RuntimeException()

class RefreshTokenExpiredException : RuntimeException()

class RateLimitExceededException(message: String) : RuntimeException(message)
