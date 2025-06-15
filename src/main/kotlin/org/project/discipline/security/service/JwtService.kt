package org.project.discipline.security.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.project.discipline.config.JwtConfig
import org.project.discipline.domain.user.entity.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*

@Service
class JwtService(private val jwtConfig: JwtConfig) {

    private val key: Key = Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())

    fun generateToken(userDetails: UserDetails): String {
        return createToken(claims = mapOf("sub" to userDetails.username))
    }

    fun generateToken(user: User): String {
        return createToken(claims = mapOf(
            "sub" to user.email,
            "name" to user.name,
            "provider" to user.provider,
            "providerId" to user.providerId
        ))
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && !isTokenExpired(token)
    }

    fun extractUsername(token: String): String {
        return extractClaim(token) { it.subject }
    }

    fun extractAllClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    private fun extractExpiration(token: String): Date {
        return extractClaim(token) { it.expiration }
    }

    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun createToken(claims: Map<String, Any>): String {
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + jwtConfig.expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun createTokenWithClaims(subject: String, claims: Map<String, Any>): String {
        val allClaims = claims.toMutableMap()
        allClaims["sub"] = subject
        return createToken(allClaims)
    }
} 