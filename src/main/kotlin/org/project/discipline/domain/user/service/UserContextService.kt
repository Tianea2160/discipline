package org.project.discipline.domain.user.service

import org.project.discipline.domain.user.dto.CurrentUser
import org.project.discipline.security.service.JwtService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class UserContextService(
    private val jwtService: JwtService
) {
    fun getCurrentUser(): CurrentUser? {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null) {
            return null
        }

        return when (val principal = authentication.principal) {
            is OAuth2User -> {
                val attributes = principal.attributes
                val provider = determineProvider(attributes)

                val email = extractEmail(attributes, provider)
                val name = extractName(attributes, provider)
                val providerId = extractProviderId(attributes, provider)
                val userId = extractUserId(attributes, provider)
                val roles = extractRoles(authentication)

                val safeEmail = email.takeIf { it.isNotEmpty() } ?: "unknown@example.com"
                val safeName = name.takeIf { it.isNotEmpty() } ?: "Unknown User"
                val safeProvider = provider.takeIf { it.isNotEmpty() } ?: "unknown"
                val safeProviderId = providerId.takeIf { it.isNotEmpty() } ?: "unknown"

                val currentUser = CurrentUser(
                    id = userId,
                    email = safeEmail,
                    name = safeName,
                    provider = safeProvider,
                    providerId = safeProviderId,
                    roles = roles
                )
                currentUser
            }

            is UserDetails -> {
                // JWT 인증을 통한 사용자 - JWT 토큰에서 추가 정보 추출
                val jwtToken = extractJwtTokenFromRequest()

                if (jwtToken != null) {
                    try {
                        val claims = jwtService.extractAllClaims(jwtToken)

                        val email = claims.subject?.takeIf { it.isNotEmpty() } ?: principal.username
                        val name = (claims["name"] as? String)?.takeIf { it.isNotEmpty() } ?: principal.username
                        val provider = (claims["provider"] as? String)?.takeIf { it.isNotEmpty() } ?: "jwt"
                        val providerId =
                            (claims["providerId"] as? String)?.takeIf { it.isNotEmpty() } ?: principal.username
                        val userId = generateIdFromEmail(email)
                        val roles = principal.authorities.map { it.authority.removePrefix("ROLE_") }

                        // 안전한 값 보장
                        val safeEmail = email?.takeIf { it.isNotEmpty() } ?: "unknown@example.com"
                        val safeName = name?.takeIf { it.isNotEmpty() } ?: "Unknown User"
                        val safeProvider = provider.takeIf { it.isNotEmpty() } ?: "unknown"
                        val safeProviderId = providerId?.takeIf { it.isNotEmpty() } ?: "unknown"

                        val currentUser = CurrentUser(
                            id = userId,
                            email = safeEmail,
                            name = safeName,
                            provider = safeProvider,
                            providerId = safeProviderId,
                            roles = roles
                        )
                        currentUser
                    } catch (e: Exception) {
                        createDefaultCurrentUser(principal)
                    }
                } else {
                    createDefaultCurrentUser(principal)
                }
            }

            else -> {
                null
            }
        }
    }

    private fun createDefaultCurrentUser(principal: UserDetails): CurrentUser {
        val email = if (principal.username.contains("@")) principal.username else "unknown@example.com"

        // 안전한 값 보장
        val safeEmail = email.takeIf { it.isNotEmpty() } ?: "unknown@example.com"
        val safeName = principal.username.takeIf { it.isNotEmpty() } ?: "Unknown User"
        val safeProviderId = principal.username.takeIf { it.isNotEmpty() } ?: "unknown"

        return CurrentUser(
            id = generateIdFromEmail(safeEmail),
            email = safeEmail,
            name = safeName,
            provider = "jwt",
            providerId = safeProviderId,
            roles = principal.authorities.map { it.authority.removePrefix("ROLE_") }
        )
    }

    private fun generateIdFromEmail(email: String): Long {
        // 이메일을 기반으로 안정적인 ID 생성 (해시코드의 절댓값 사용)
        return kotlin.math.abs(email.hashCode().toLong())
    }

    private fun extractJwtTokenFromRequest(): String? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val authHeader = request?.getHeader("Authorization")
        return if (authHeader?.startsWith("Bearer ") == true) {
            authHeader.substring(7)
        } else null
    }

    private fun determineProvider(attributes: Map<String, Any>): String {
        return when {
            attributes.containsKey("sub") && attributes.containsKey("email_verified") -> "google"
            attributes.containsKey("sub") && attributes.containsKey("is_private_email") -> "apple"
            else -> "unknown"
        }
    }

    private fun extractUserId(attributes: Map<String, Any>, provider: String): Long {
        val providerId = extractProviderId(attributes, provider)
        val email = extractEmail(attributes, provider)
        val identifier = providerId.ifEmpty { email }
        return if (identifier.isNotEmpty()) {
            kotlin.math.abs(identifier.hashCode().toLong())
        } else {
            // 최후의 수단으로 현재 시간 기반 ID 생성
            System.currentTimeMillis() % 1000000
        }
    }

    private fun extractEmail(attributes: Map<String, Any>, provider: String): String {
        return when (provider) {
            "google" -> attributes["email"] as? String ?: ""
            "apple" -> attributes["email"] as? String ?: ""
            else -> ""
        }
    }

    private fun extractName(attributes: Map<String, Any>, provider: String): String {
        return when (provider) {
            "google" -> attributes["name"] as? String ?: ""
            "apple" -> {
                val name = attributes["name"] as? Map<String, Any>
                val firstName = name?.get("firstName") as? String ?: ""
                val lastName = name?.get("lastName") as? String ?: ""
                "$firstName $lastName".trim().takeIf { it.isNotEmpty() } ?: ""
            }

            else -> ""
        }
    }

    private fun extractProviderId(attributes: Map<String, Any>, provider: String): String {
        return attributes["sub"] as? String ?: ""
    }

    private fun extractRoles(authentication: Authentication): List<String> {
        return authentication.authorities?.map { it.authority.removePrefix("ROLE_") } ?: listOf("USER")
    }
}