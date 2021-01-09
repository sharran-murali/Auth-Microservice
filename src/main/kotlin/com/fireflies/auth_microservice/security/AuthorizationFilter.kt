package com.fireflies.auth_microservice.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fireflies.auth_microservice.model.UserCredential
import com.fireflies.auth_microservice.repository_service.UserCredentialRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthorizationFilter(authenticationManager: AuthenticationManager, private val userRepository: UserCredentialRepository): BasicAuthenticationFilter(authenticationManager) {

    @Throws(Exception::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header = request.getHeader(JwtProperties.AUTHORIZATION)
        if (header == null || !header.startsWith(JwtProperties.BEARER_)) {
            chain.doFilter(request, response)
            return
        }
        val authentication = getUsernamePasswordAuthentication(request)
        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(request, response)
    }

    private fun getUsernamePasswordAuthentication(request: HttpServletRequest): Authentication? {
        val token = request.getHeader(JwtProperties.AUTHORIZATION).replace(JwtProperties.BEARER_, "")
        val username = JWT.require(Algorithm.HMAC512(JwtProperties.SECRET.toByteArray()))
            .build()
            .verify(token)
            .subject
        if (username != null) {
            val user: Optional<UserCredential> = userRepository.findByUsername(username)
            return if (user.isPresent) {
                val principal = UserPrincipal(user.get())
                UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
            } else {
                null
            }
        }
        return null
    }
}