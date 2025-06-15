package org.project.discipline.domain.user.service

import org.project.discipline.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {
    
    private val logger = LoggerFactory.getLogger(CustomUserDetailsService::class.java)

    override fun loadUserByUsername(username: String): UserDetails {
        logger.debug("CustomUserDetailsService: loadUserByUsername called with: $username")
        
        // JWT 토큰에서 추출된 username(email)으로 사용자 조회
        val user = userRepository.findByEmail(username)
        logger.debug("CustomUserDetailsService: Found user in DB: ${user != null}")
        
        return if (user != null) {
            User.builder()
                .username(user.email)
                .password("") // OAuth2 사용자는 비밀번호가 없음
                .authorities(listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")))
                .build()
        } else {
            // OAuth2LoginSuccessHandler에서 사용자가 생성되므로 이 경우는 드물어야 함
            logger.warn("CustomUserDetailsService: User not found for email: $username")
            User.builder()
                .username(username)
                .password("")
                .authorities(listOf(SimpleGrantedAuthority("ROLE_USER")))
                .build()
        }
    }
}