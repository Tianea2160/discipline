package org.project.discipline.config

import org.project.discipline.domain.user.service.UserContextService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class AuditConfig {

    @Bean
    fun auditorProvider(): AuditorAware<String> = AuditorAwareImpl()
}

/**
 * 현재 사용자 정보를 제공하는 AuditorAware 구현체
 */
class AuditorAwareImpl : AuditorAware<String> {
    
    override fun getCurrentAuditor(): Optional<String> {
        return try {
            val authentication: Authentication? = SecurityContextHolder.getContext().authentication
            
            when {
                // 인증되지 않은 경우
                authentication == null || !authentication.isAuthenticated -> {
                    Optional.of("anonymous")
                }
                
                // 익명 사용자인 경우
                authentication.name == "anonymousUser" -> {
                    Optional.of("anonymous")
                }
                
                // 인증된 사용자인 경우
                else -> {
                    // JWT에서 사용자 ID나 이메일을 추출
                    val principal = authentication.principal
                    val username = when (principal) {
                        is String -> principal
                        else -> authentication.name
                    }
                    Optional.of(username ?: "anonymous")
                }
            }
        } catch (e: Exception) {
            // 예외 발생 시 anonymous로 처리
            Optional.of("anonymous")
        }
    }
} 