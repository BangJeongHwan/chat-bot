package com.stockbot.account.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "accounts")
class Account(
    @Id
    @Column(length = 36)
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", length = 36, nullable = false)
    val userId: String,

    @Column(length = 100, nullable = false)
    val name: String,

    @Column(name = "balance_krw", nullable = false, precision = 18, scale = 2)
    var balanceKrw: BigDecimal,

    @Column(name = "balance_usd", nullable = false, precision = 18, scale = 4)
    var balanceUsd: BigDecimal,

    @Column(name = "initial_balance", nullable = false, precision = 18, scale = 2)
    val initialBalance: BigDecimal,

    @Column(name = "is_default", nullable = false)
    val isDefault: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
