package org.project.discipline.domain.user.service

import org.project.discipline.domain.user.dto.CurrentUser
import org.project.discipline.security.service.JwtService
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(UserContextService::class.java)

    fun getCurrentUser(): CurrentUser? {
        logger.debug("=== UserContextService: getCurrentUser 시작 ===")
        
        val authentication = SecurityContextHolder.getContext().authentication
        logger.debug("UserContextService: Authentication 객체: {}", authentication)
        
        if (authentication == null) {
            logger.warn("UserContextService: Authentication이 null입니다.")
            return null
        }
        
        logger.debug("UserContextService: Principal 타입: ${authentication.principal?.javaClass?.simpleName}")
        logger.debug("UserContextService: Principal 내용: ${authentication.principal}")
        logger.debug("UserContextService: Authorities: ${authentication.authorities}")

        return when (val principal = authentication.principal) {
            is OAuth2User -> {
                logger.debug("UserContextService: OAuth2User 처리 시작")
                val attributes = principal.attributes
                logger.debug("UserContextService: OAuth2 attributes: {}", attributes)
                
                val provider = determineProvider(attributes)
                logger.debug("UserContextService: 감지된 provider: $provider")
                
                val email = extractEmail(attributes, provider)
                val name = extractName(attributes, provider)
                val providerId = extractProviderId(attributes, provider)
                val userId = extractUserId(attributes, provider)
                val roles = extractRoles(authentication)
                
                logger.debug("UserContextService: 추출된 정보 - email: '$email', name: '$name', providerId: '$providerId', userId: $userId, roles: $roles")
                
                // 안전한 값 보장
                val safeEmail = email.takeIf { it.isNotEmpty() } ?: "unknown@example.com"
                val safeName = name.takeIf { it.isNotEmpty() } ?: "Unknown User"
                val safeProvider = provider.takeIf { it.isNotEmpty() } ?: "unknown"
                val safeProviderId = providerId.takeIf { it.isNotEmpty() } ?: "unknown"
                
                logger.debug("UserContextService: OAuth2 안전한 값들 - email: '$safeEmail', name: '$safeName', provider: '$safeProvider', providerId: '$safeProviderId'")
                
                val currentUser = CurrentUser(
                    id = userId,
                    email = safeEmail,
                    name = safeName,
                    provider = safeProvider,
                    providerId = safeProviderId,
                    roles = roles
                )
                logger.debug("UserContextService: OAuth2User에서 생성된 CurrentUser: $currentUser")
                currentUser
            }
            is UserDetails -> {
                logger.debug("UserContextService: UserDetails 처리 시작")
                logger.debug("UserContextService: UserDetails username: '${principal.username}'")
                logger.debug("UserContextService: UserDetails authorities: ${principal.authorities}")
                
                // JWT 인증을 통한 사용자 - JWT 토큰에서 추가 정보 추출
                val jwtToken = extractJwtTokenFromRequest()
                logger.debug("UserContextService: 추출된 JWT 토큰: ${jwtToken?.take(50)}...")
                
                if (jwtToken != null) {
                    try {
                        val claims = jwtService.extractAllClaims(jwtToken)
                        logger.debug("UserContextService: JWT Claims: $claims")
                        
                        val email = claims.subject?.takeIf { it.isNotEmpty() } ?: principal.username
                        val name = (claims["name"] as? String)?.takeIf { it.isNotEmpty() } ?: principal.username
                        val provider = (claims["provider"] as? String)?.takeIf { it.isNotEmpty() } ?: "jwt"
                        val providerId = (claims["providerId"] as? String)?.takeIf { it.isNotEmpty() } ?: principal.username
                        val userId = generateIdFromEmail(email)
                        val roles = principal.authorities.map { it.authority.removePrefix("ROLE_") }
                        
                        logger.debug("UserContextService: JWT에서 추출된 정보 - email: '$email', name: '$name', provider: '$provider', providerId: '$providerId', userId: $userId, roles: $roles")
                        
                        // 안전한 값 보장
                        val safeEmail = email?.takeIf { it.isNotEmpty() } ?: "unknown@example.com"
                        val safeName = name?.takeIf { it.isNotEmpty() } ?: "Unknown User"
                        val safeProvider = provider?.takeIf { it.isNotEmpty() } ?: "unknown"
                        val safeProviderId = providerId?.takeIf { it.isNotEmpty() } ?: "unknown"
                        
                        logger.debug("UserContextService: 안전한 값들 - email: '$safeEmail', name: '$safeName', provider: '$safeProvider', providerId: '$safeProviderId'")
                        
                        val currentUser = CurrentUser(
                            id = userId,
                            email = safeEmail,
                            name = safeName,
                            provider = safeProvider,
                            providerId = safeProviderId,
                            roles = roles
                        )
                        logger.debug("UserContextService: JWT에서 생성된 CurrentUser: $currentUser")
                        currentUser
                    } catch (e: Exception) {
                        logger.error("UserContextService: JWT 파싱 실패", e)
                        // JWT 파싱 실패 시 기본 정보 사용
                        createDefaultCurrentUser(principal)
                    }
                } else {
                    logger.debug("UserContextService: JWT 토큰이 없어서 기본 정보 사용")
                    createDefaultCurrentUser(principal)
                }
            }
            else -> {
                logger.warn("UserContextService: 지원하지 않는 Principal 타입: ${principal?.javaClass}")
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
        // 실제로는 데이터베이스에서 사용자 ID를 조회해야 합니다
        // 여기서는 임시로 providerId의 해시값을 사용
        val providerId = extractProviderId(attributes, provider)
        val email = extractEmail(attributes, provider)
        
        // providerId가 있으면 그것을 사용하고, 없으면 이메일을 사용
        val identifier = if (providerId.isNotEmpty()) providerId else email
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

    private fun extractRoles(authentication: org.springframework.security.core.Authentication): List<String> {
        return authentication.authorities?.map { it.authority.removePrefix("ROLE_") } ?: listOf("USER")
    }

    fun requireCurrentUser(): CurrentUser {
        return getCurrentUser() 
            ?: throw IllegalStateException("사용자 인증 정보를 찾을 수 없습니다.")
    }
}