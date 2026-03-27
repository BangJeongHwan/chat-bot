package com.stockbot.auth.controller

import com.stockbot.auth.dto.AuthResponse
import com.stockbot.auth.dto.LoginRequest
import com.stockbot.auth.dto.RegisterRequest
import com.stockbot.auth.service.AuthService
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val (authResponse, rawRefreshToken) = authService.register(request)
        addRefreshTokenCookie(response, rawRefreshToken)
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val (authResponse, rawRefreshToken) = authService.login(request)
        addRefreshTokenCookie(response, rawRefreshToken)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refresh_token") refreshToken: String,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val (authResponse, rawRefreshToken) = authService.refresh(refreshToken)
        addRefreshTokenCookie(response, rawRefreshToken)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue("refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        if (refreshToken != null) {
            authService.logout(refreshToken)
        }
        clearRefreshTokenCookie(response)
        return ResponseEntity.noContent().build()
    }

    private fun addRefreshTokenCookie(response: HttpServletResponse, rawToken: String) {
        val cookie = ResponseCookie.from("refresh_token", rawToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ofDays(7))
            .build()
        response.addHeader("Set-Cookie", cookie.toString())
    }

    private fun clearRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ZERO)
            .build()
        response.addHeader("Set-Cookie", cookie.toString())
    }
}
