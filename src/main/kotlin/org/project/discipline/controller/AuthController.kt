package org.project.discipline.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.project.discipline.annotation.CurrentUserInfo
import org.project.discipline.domain.user.dto.CurrentUser
import org.project.discipline.domain.user.dto.OAuthLoginRequest
import org.project.discipline.domain.user.dto.OAuthLoginResponse
import org.project.discipline.domain.user.service.OAuthLoginService
import org.project.discipline.domain.user.service.UserContextService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증 API", description = "사용자 인증 관련 API")
class AuthController(
    private val userContextService: UserContextService,
    private val oAuthLoginService: OAuthLoginService
) {

    @PostMapping("/oauth-login")
    @Operation(
        summary = "클라이언트 OAuth 로그인",
        description = "클라이언트에서 OAuth SDK로 로그인한 결과를 서버에 전달하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun oauthLogin(
        @Valid @RequestBody request: OAuthLoginRequest
    ): ResponseEntity<OAuthLoginResponse> {
        val response = oAuthLoginService.processOAuthLogin(request)

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/providers")
    @Operation(
        summary = "지원되는 OAuth 제공자 목록",
        description = "현재 시스템에서 지원하는 OAuth 제공자 목록을 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "제공자 목록 조회 성공")
        ]
    )
    fun getSupportedProviders(): ResponseEntity<Map<String, Any>> {
        val providers = oAuthLoginService.getSupportedProviders()

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "providers" to providers,
                "message" to "지원되는 OAuth 제공자 목록",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    @GetMapping("/me")
    @Operation(
        summary = "현재 사용자 정보 조회",
        description = "JWT 토큰을 통해 현재 로그인한 사용자의 정보를 조회합니다.",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
        ]
    )
    fun getCurrentUser(
        @Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser?
    ): ResponseEntity<Map<String, Any>> {
        // AOP가 작동하지 않는 경우를 대비한 fallback
        val user = currentUser ?: userContextService.getCurrentUser()

        if (user == null) {
            return ResponseEntity.ok(
                mapOf(
                    "success" to false,
                    "message" to "사용자 인증 정보를 찾을 수 없습니다.",
                    "aopWorking" to false,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "user" to mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "name" to user.name,
                    "provider" to user.provider,
                    "providerId" to user.providerId,
                    "roles" to user.roles,
                    "isAdmin" to user.isAdmin()
                ),
                "aopWorking" to (currentUser != null),
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    @GetMapping("/test-aop")
    @Operation(
        summary = "AOP 테스트",
        description = "AOP를 통한 사용자 정보 주입이 정상적으로 작동하는지 테스트합니다.",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "AOP 테스트 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
        ]
    )
    fun testAop(
        @Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser?
    ): ResponseEntity<Map<String, Any?>?> {
        return ResponseEntity.ok(
            mapOf(
                "aopInjected" to (currentUser != null),
                "currentUser" to currentUser,
                "manualUser" to userContextService.getCurrentUser(),
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
} 