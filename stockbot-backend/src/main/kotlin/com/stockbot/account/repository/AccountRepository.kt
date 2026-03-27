package com.stockbot.account.repository

import com.stockbot.account.domain.Account
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, String> {
    fun findByUserIdAndIsDefaultTrue(userId: String): Account?
    fun findAllByUserId(userId: String): List<Account>
}
