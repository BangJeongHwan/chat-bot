package com.stockbot.account.controller

import com.stockbot.account.service.AccountService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class AccountResponse(
    val id: String,
    val name: String,
    val balanceKrw: BigDecimal,
    val balanceUsd: BigDecimal,
    val isDefault: Boolean
)

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountService: AccountService
) {
    @GetMapping
    fun getMyAccounts(authentication: Authentication): ResponseEntity<List<AccountResponse>> {
        val userId = authentication.name
        val accounts = accountService.getAccountsByUserId(userId)
        val response = accounts.map { account ->
            AccountResponse(
                id = account.id,
                name = account.name,
                balanceKrw = account.balanceKrw,
                balanceUsd = account.balanceUsd,
                isDefault = account.isDefault
            )
        }
        return ResponseEntity.ok(response)
    }
}
