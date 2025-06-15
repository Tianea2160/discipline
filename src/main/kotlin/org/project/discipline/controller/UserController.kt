package org.project.discipline.controller

import io.swagger.v3.oas.annotations.Parameter
import org.project.discipline.annotation.CurrentUserInfo
import org.project.discipline.annotation.RequireAuth
import org.project.discipline.domain.user.dto.CurrentUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController {

    @GetMapping("/profile")
    @RequireAuth
    fun getUserProfile(@Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser): Map<String, Any?> {
        return mapOf(
            "id" to currentUser.id,
            "email" to currentUser.email,
            "name" to currentUser.name,
            "provider" to currentUser.provider,
            "roles" to currentUser.roles
        )
    }

    @GetMapping("/admin")
    @RequireAuth(roles = ["ADMIN"])
    fun getAdminInfo(@Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser): Map<String, Any> {
        return mapOf(
            "message" to "관리자 전용 정보입니다.",
            "adminUser" to currentUser.name
        )
    }

    @GetMapping("/dashboard")
    fun getDashboard(@Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser): Map<String, Any> {
        return mapOf(
            "welcome" to "안녕하세요, ${currentUser.name}님!",
            "provider" to "${currentUser.provider} 계정으로 로그인하셨습니다.",
            "email" to currentUser.email
        )
    }

    @GetMapping("/settings")
    @RequireAuth
    fun getUserSettings(@Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser): Map<String, Any?> {
        return mapOf(
            "userId" to currentUser.id,
            "email" to currentUser.email,
            "name" to currentUser.name,
            "provider" to currentUser.provider,
            "isAdmin" to currentUser.isAdmin(),
            "availableSettings" to listOf(
                "프로필 수정",
                "알림 설정",
                "개인정보 설정"
            )
        )
    }
} 