package com.stockbot.auth.repository

import com.stockbot.auth.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun deleteByUserId(userId: String)
    fun deleteByTokenHash(tokenHash: String)
}
