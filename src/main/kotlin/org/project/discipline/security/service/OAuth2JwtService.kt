package org.project.discipline.security.service

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class OAuth2JwtService(
    private val jwtService: JwtService
) {
    fun generateToken(authentication: Authentication): String {
        val oauth2User = authentication.principal as OAuth2User
        val provider = determineProvider(oauth2User.attributes)

        val (email, name, providerId) = when (provider) {
            "google" -> extractGoogleUserInfo(oauth2User)
            "apple" -> extractAppleUserInfo(oauth2User)
            else -> throw IllegalStateException("Unsupported provider: $provider")
        }

        // User 엔티티 대신 직접 JWT 토큰 생성
        return jwtService.createTokenWithClaims(
            subject = email,
            claims = mapOf(
                "name" to name,
                "provider" to provider,
                "providerId" to providerId,
                "email" to email
            )
        )
    }

    private fun determineProvider(attributes: Map<String, Any>): String {
        return when {
            attributes.containsKey("sub") && attributes.containsKey("email_verified") -> "google"
            attributes.containsKey("sub") && attributes.containsKey("is_private_email") -> "apple"
            else -> "unknown"
        }
    }

    private fun extractGoogleUserInfo(oauth2User: OAuth2User): UserInfo {
        val email = oauth2User.getAttribute<String>("email")
            ?: throw IllegalStateException("Email not found from Google")
        val name = oauth2User.getAttribute<String>("name")
            ?: throw IllegalStateException("Name not found from Google")
        val providerId = oauth2User.getAttribute<String>("sub")
            ?: throw IllegalStateException("Provider ID not found from Google")

        return UserInfo(email, name, providerId)
    }

    private fun extractAppleUserInfo(oauth2User: OAuth2User): UserInfo {
        val email = oauth2User.getAttribute<String>("email")
            ?: throw IllegalStateException("Email not found from Apple")
        val name = oauth2User.getAttribute<String>("name")
            ?: oauth2User.getAttribute<String>("sub")
            ?: throw IllegalStateException("Name not found from Apple")
        val providerId = oauth2User.getAttribute<String>("sub")
            ?: throw IllegalStateException("Provider ID not found from Apple")

        return UserInfo(email, name, providerId)
    }

    private data class UserInfo(
        val email: String,
        val name: String,
        val providerId: String
    )
} 