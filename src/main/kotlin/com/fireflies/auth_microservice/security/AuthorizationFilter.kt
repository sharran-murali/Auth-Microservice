package com.fireflies.auth_microservice.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fireflies.auth_microservice.AppProperties
import com.fireflies.auth_microservice.repository_service.UserService
import com.fireflies.auth_microservice.util.log
import com.fireflies.auth_microservice.util.objectMapper
import kotlinx.coroutines.runBlocking
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthorizationFilter(
    authenticationManager: AuthenticationManager,
    private val userService: UserService
) : BasicAuthenticationFilter(authenticationManager) {

    @Throws(Exception::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header = request.getHeader(AppProperties.Security.AUTHORIZATION)
        if (header == null || !header.startsWith(AppProperties.Security.BEARER_)) {
            chain.doFilter(request, response)
            return
        }
        val authentication = getUsernamePasswordAuthentication(request)
        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(request, response)
    }

    private fun getUsernamePasswordAuthentication(request: HttpServletRequest) = runBlocking<Authentication?> {
        val token = request.getHeader(AppProperties.Security.AUTHORIZATION).replace(AppProperties.Security.BEARER_, "")
        JWT.require(Algorithm.HMAC512(AppProperties.Security.SECRET.toByteArray()))
            .build()
            .verify(token)
            .subject
            .let { username ->
                userService
                    .findActiveUser(username)
                    ?.takeIf { it.isLoggedIn }
                    ?.let {
                        val principal = UserPrincipal(it)
                        UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                    }
            }
    }
}