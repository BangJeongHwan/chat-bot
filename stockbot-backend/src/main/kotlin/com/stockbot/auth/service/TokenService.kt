package com.stockbot.auth.service

import com.stockbot.auth.domain.RefreshToken
import com.stockbot.auth.dto.TokenPair
import com.stockbot.auth.repository.RefreshTokenRepository
import com.stockbot.common.exception.RefreshTokenExpiredException
import com.stockbot.common.exception.RefreshTokenInvalidException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class TokenService(
    @Value("\${jwt.secret}") secret: String,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun createAccessToken(userId: String, email: String, isVerified: Boolean): String {
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("is_verified", isVerified)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 15 * 60 * 1000))
            .signWith(key)
            .compact()
    }

    fun validateAccessToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun issueTokenPair(userId: String, email: String, isVerified: Boolean): TokenPair {
        val accessToken = createAccessToken(userId, email, isVerified)
        val rawRefreshToken = UUID.randomUUID().toString()
        val tokenHash = sha256(rawRefreshToken)
        val expiresAt = LocalDateTime.now().plusDays(7)

        val refreshToken = RefreshToken(
            userId = userId,
            tokenHash = tokenHash,
            expiresAt = expiresAt
        )
        refreshTokenRepository.save(refreshToken)

        return TokenPair(accessToken, rawRefreshToken)
    }

    /**
     * Validates the raw refresh token, deletes all tokens for the user (rotation),
     * and returns the userId so the caller can look up the user and issue a new token pair.
     */
    fun validateAndInvalidateRefreshToken(rawToken: String): String {
        val tokenHash = sha256(rawToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw RefreshTokenInvalidException()

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken)
            throw RefreshTokenExpiredException()
        }

        // Rotation: delete ALL tokens for this user (per D-04)
        refreshTokenRepository.deleteByUserId(storedToken.userId)

        return storedToken.userId
    }

    fun rotateRefreshToken(rawToken: String): Pair<TokenPair, String> {
        val tokenHash = sha256(rawToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw RefreshTokenInvalidException()

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken)
            throw RefreshTokenExpiredException()
        }

        // Rotation: delete ALL tokens for this user (per D-04)
        refreshTokenRepository.deleteByUserId(storedToken.userId)

        return Pair(TokenPair("", ""), storedToken.userId)
    }

    fun invalidateRefreshToken(rawToken: String) {
        val tokenHash = sha256(rawToken)
        refreshTokenRepository.deleteByTokenHash(tokenHash)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
