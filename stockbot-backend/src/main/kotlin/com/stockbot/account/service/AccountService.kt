package com.stockbot.account.service

import com.stockbot.account.domain.Account
import com.stockbot.account.repository.AccountRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class AccountService(
    private val accountRepository: AccountRepository
) {
    fun createDefaultAccount(userId: String): Account {
        val account = Account(
            userId = userId,
            name = "기본 계좌",
            balanceKrw = BigDecimal("100000000"),
            balanceUsd = BigDecimal.ZERO,
            initialBalance = BigDecimal("100000000"),
            isDefault = true
        )
        return accountRepository.save(account)
    }

    fun getAccountsByUserId(userId: String): List<Account> {
        return accountRepository.findAllByUserId(userId)
    }
}
