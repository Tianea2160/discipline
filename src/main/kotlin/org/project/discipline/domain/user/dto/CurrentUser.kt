package org.project.discipline.domain.user.dto

open class CurrentUser(
    val id: Long?,
    val email: String,
    val name: String,
    val provider: String,
    val providerId: String,
    val roles: List<String> = emptyList()
) {
    fun hasRole(role: String): Boolean = roles.contains(role)
    fun isAdmin(): Boolean = hasRole("ADMIN")
}