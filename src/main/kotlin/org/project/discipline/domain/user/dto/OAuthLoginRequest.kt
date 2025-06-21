package org.project.discipline.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "클라이언트 OAuth 로그인 요청")
data class OAuthLoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "사용자 이름은 필수입니다")
    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String,

    @field:NotBlank(message = "OAuth 제공자는 필수입니다")
    @Schema(description = "OAuth 제공자 (google, apple 등)", example = "google")
    val provider: String,

    @field:NotBlank(message = "제공자 사용자 ID는 필수입니다")
    @Schema(description = "OAuth 제공자에서 제공하는 사용자 고유 ID", example = "123456789")
    val providerId: String,

    @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val picture: String? = null
) 