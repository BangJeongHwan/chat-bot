package com.stockbot.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class RegisterRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 50)
    val password: String
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 50)
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean,
    val createdAt: LocalDateTime
)

data class TokenPair(
    val accessToken: String,
    val rawRefreshToken: String
)
