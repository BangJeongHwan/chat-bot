package com.stockbot.auth.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @Column(length = 36)
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(length = 50)
    var nickname: String? = null,

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,

    @Column(length = 20, nullable = false)
    val provider: String = "local",

    @Column(name = "provider_id")
    var providerId: String? = null,

    @Column(name = "invest_style", length = 20)
    var investStyle: String? = null,

    @Column(nullable = false)
    var level: Int = 1,

    @Column(nullable = false)
    var experience: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
