package org.project.discipline.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.project.discipline.domain.user.entity.User
import org.project.discipline.domain.user.entity.UserRole
import org.project.discipline.domain.user.repository.UserRepository
import org.project.discipline.security.service.OAuth2JwtService
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginSuccessHandler(
    private val oauth2JwtService: OAuth2JwtService,
    private val userRepository: UserRepository
) : SimpleUrlAuthenticationSuccessHandler() {

    private val logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler::class.java)

    init {
        setDefaultTargetUrl("/hello")
    }

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        try {
            // OAuth2 사용자 정보 추출
            val oauth2User = authentication.principal as OAuth2User
            val userInfo = extractUserInfo(oauth2User)
            
            logger.info("OAuth2 로그인 성공: email=${userInfo.email}, name=${userInfo.name}, provider=${userInfo.provider}")
            
            // 데이터베이스에서 사용자 조회 또는 생성
            val user = findOrCreateUser(userInfo)
            logger.info("사용자 처리 완료: id=${user.id}, email=${user.email}")
            
            // JWT 토큰 생성
            val token = oauth2JwtService.generateToken(authentication)

            response.setHeader("Authorization", "Bearer $token")

            val targetUrl = "/hello?token=$token"
            redirectStrategy.sendRedirect(request, response, targetUrl)
        } catch (e: Exception) {
            logger.error("OAuth2 로그인 성공 후 처리 실패", e)
            super.onAuthenticationSuccess(request, response, authentication)
        }
    }
    
    private fun extractUserInfo(oauth2User: OAuth2User): UserInfo {
        val attributes = oauth2User.attributes
        val provider = determineProvider(attributes)
        
        return when (provider) {
            "google" -> extractGoogleUserInfo(attributes)
            "apple" -> extractAppleUserInfo(attributes)
            else -> throw IllegalStateException("지원하지 않는 OAuth2 제공자: $provider")
        }
    }
    
    private fun determineProvider(attributes: Map<String, Any>): String {
        return when {
            attributes.containsKey("sub") && attributes.containsKey("email_verified") -> "google"
            attributes.containsKey("sub") && attributes.containsKey("is_private_email") -> "apple"
            else -> "unknown"
        }
    }
    
    private fun extractGoogleUserInfo(attributes: Map<String, Any>): UserInfo {
        val email = attributes["email"] as? String
            ?: throw IllegalStateException("Google에서 이메일을 찾을 수 없습니다")
        val name = attributes["name"] as? String
            ?: throw IllegalStateException("Google에서 이름을 찾을 수 없습니다")
        val providerId = attributes["sub"] as? String
            ?: throw IllegalStateException("Google에서 Provider ID를 찾을 수 없습니다")
        val picture = attributes["picture"] as? String
        
        return UserInfo(email, name, "google", providerId, picture)
    }
    
    private fun extractAppleUserInfo(attributes: Map<String, Any>): UserInfo {
        val email = attributes["email"] as? String
            ?: throw IllegalStateException("Apple에서 이메일을 찾을 수 없습니다")
        val name = attributes["name"] as? String
            ?: attributes["sub"] as? String
            ?: throw IllegalStateException("Apple에서 이름을 찾을 수 없습니다")
        val providerId = attributes["sub"] as? String
            ?: throw IllegalStateException("Apple에서 Provider ID를 찾을 수 없습니다")
        
        return UserInfo(email, name, "apple", providerId, null)
    }
    
    private fun findOrCreateUser(userInfo: UserInfo): User {
        // 먼저 이메일로 사용자 조회
        var user = userRepository.findByEmail(userInfo.email)
        
        if (user == null) {
            // 사용자가 없으면 새로 생성
            logger.info("새 사용자 생성: email=${userInfo.email}, provider=${userInfo.provider}")
            user = User(
                email = userInfo.email,
                name = userInfo.name,
                picture = userInfo.picture,
                provider = userInfo.provider,
                providerId = userInfo.providerId,
                role = UserRole.USER
            )
            user = userRepository.save(user)
            logger.info("새 사용자 생성 완료: id=${user.id}")
        } else {
            logger.info("기존 사용자 발견: id=${user.id}, email=${user.email}")
        }
        
        return user
    }
    
    private data class UserInfo(
        val email: String,
        val name: String,
        val provider: String,
        val providerId: String,
        val picture: String?
    )
} 