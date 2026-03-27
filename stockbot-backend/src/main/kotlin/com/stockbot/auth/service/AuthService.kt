package com.stockbot.auth.service

import com.stockbot.account.service.AccountService
import com.stockbot.auth.domain.User
import com.stockbot.auth.dto.AuthResponse
import com.stockbot.auth.dto.RegisterRequest
import com.stockbot.auth.dto.LoginRequest
import com.stockbot.auth.dto.UserResponse
import com.stockbot.auth.repository.UserRepository
import com.stockbot.common.exception.EmailAlreadyExistsException
import com.stockbot.common.exception.InvalidCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val accountService: AccountService,
    private val tokenService: TokenService,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun register(request: RegisterRequest): Pair<AuthResponse, String> {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException()
        }

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )
        userRepository.save(user)

        accountService.createDefaultAccount(user.id)

        val tokenPair = tokenService.issueTokenPair(user.id, user.email, user.isVerified)
        val authResponse = AuthResponse(
            accessToken = tokenPair.accessToken,
            user = user.toUserResponse()
        )
        return Pair(authResponse, tokenPair.rawRefreshToken)
    }

    fun login(request: LoginRequest): Pair<AuthResponse, String> {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val tokenPair = tokenService.issueTokenPair(user.id, user.email, user.isVerified)
        val authResponse = AuthResponse(
            accessToken = tokenPair.accessToken,
            user = user.toUserResponse()
        )
        return Pair(authResponse, tokenPair.rawRefreshToken)
    }

    fun refresh(rawRefreshToken: String): Pair<AuthResponse, String> {
        val userId = tokenService.validateAndInvalidateRefreshToken(rawRefreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { InvalidCredentialsException() }

        val newTokenPair = tokenService.issueTokenPair(user.id, user.email, user.isVerified)
        val authResponse = AuthResponse(
            accessToken = newTokenPair.accessToken,
            user = user.toUserResponse()
        )
        return Pair(authResponse, newTokenPair.rawRefreshToken)
    }

    @Transactional
    fun logout(rawRefreshToken: String) {
        tokenService.invalidateRefreshToken(rawRefreshToken)
    }

    private fun User.toUserResponse() = UserResponse(
        id = id,
        email = email,
        isVerified = isVerified,
        createdAt = createdAt
    )
}
