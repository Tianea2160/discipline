package org.project.discipline.domain.user.repository

import org.project.discipline.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByProviderAndProviderId(provider: String, providerId: String): User?
    
    /**
     * 이메일과 provider로 사용자 조회
     * 같은 이메일이라도 다른 provider면 다른 계정으로 취급
     */
    fun findByEmailAndProvider(email: String, provider: String): User?
    
    /**
     * 특정 이메일을 사용하는 모든 사용자 조회 (모든 provider 포함)
     */
    fun findAllByEmail(email: String): List<User>
} 