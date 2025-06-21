package org.project.discipline.domain.user.entity

import jakarta.persistence.*
import org.project.discipline.domain.common.entity.BaseTimeEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        // provider + email 조합은 유일해야 함 (같은 이메일, 같은 provider 중복 방지)
        UniqueConstraint(
            name = "uk_users_provider_email",
            columnNames = ["provider", "email"]
        )
    ],
    indexes = [
        // 자주 사용되는 검색 조건들에 대한 인덱스
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_provider", columnList = "provider")
    ]
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var name: String,

    @Column
    var picture: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,

    @Column(nullable = false)
    val provider: String,

    @Column(name = "provider_id", nullable = false, unique = true)
    val providerId: String,
) : UserDetails, BaseTimeEntity() {
    override fun getAuthorities(): Collection<GrantedAuthority> =
        mutableListOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun getPassword(): String = ""
    override fun getUsername(): String = name
    
    /**
     * OAuth 로그인 시 사용자 정보 업데이트
     */
    fun updateOAuthInfo(newEmail: String, newName: String, newPicture: String?) {
        this.email = newEmail
        this.name = newName
        this.picture = newPicture
    }
}

enum class UserRole {
    USER, ADMIN
} 