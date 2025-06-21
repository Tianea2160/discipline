package org.project.discipline.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "클라이언트 OAuth 로그인 응답")
data class OAuthLoginResponse(
    @Schema(description = "성공 여부")
    val success: Boolean,

    @Schema(description = "JWT 토큰")
    val token: String?,

    @Schema(description = "사용자 정보")
    val user: UserInfo?,

    @Schema(description = "응답 메시지")
    val message: String,

    @Schema(description = "새로 생성된 사용자인지 여부")
    val isNewUser: Boolean = false,

    @Schema(description = "응답 시간")
    val timestamp: Long = System.currentTimeMillis()
) {
    @Schema(description = "사용자 기본 정보")
    data class UserInfo(
        @Schema(description = "사용자 ID")
        val id: Long,

        @Schema(description = "이메일")
        val email: String,

        @Schema(description = "사용자 이름")
        val name: String,

        @Schema(description = "OAuth 제공자")
        val provider: String,

        @Schema(description = "제공자 사용자 ID")
        val providerId: String,

        @Schema(description = "프로필 이미지 URL")
        val picture: String?,

        @Schema(description = "사용자 역할")
        val roles: List<String>,

        @Schema(description = "관리자 여부")
        val isAdmin: Boolean
    )
} 