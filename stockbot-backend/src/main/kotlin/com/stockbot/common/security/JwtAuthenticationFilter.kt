package com.stockbot.common.security

import com.stockbot.auth.service.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenService: TokenService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI

        // Skip JWT filter for public auth endpoints
        if (requestPath.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            try {
                val claims = tokenService.validateAccessToken(token)
                val authentication = UsernamePasswordAuthenticationToken(
                    claims.subject,
                    null,
                    emptyList()
                )
                SecurityContextHolder.getContext().authentication = authentication
            } catch (ex: Exception) {
                // Invalid or expired token — continue without setting auth
                // Spring Security will return 401 for protected endpoints
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }
}
