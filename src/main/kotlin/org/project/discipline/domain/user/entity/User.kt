package org.project.discipline.domain.user.entity

import jakarta.persistence.*
import org.project.discipline.domain.common.entity.BaseAuditEntity
import org.project.discipline.domain.common.entity.BaseTimeEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val name: String,

    @Column
    val picture: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,

    @Column(nullable = false)
    val provider: String,

    @Column(nullable = false)
    val providerId: String,
) : UserDetails, BaseTimeEntity() {
    override fun getAuthorities(): Collection<GrantedAuthority> =
        mutableListOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun getPassword(): String = ""
    override fun getUsername(): String = name
}

enum class UserRole {
    USER, ADMIN
} 