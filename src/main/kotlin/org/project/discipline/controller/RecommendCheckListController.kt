package org.project.discipline.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.project.discipline.annotation.CurrentUserInfo
import org.project.discipline.domain.checklist.dto.RecommendCheckListRequest
import org.project.discipline.domain.checklist.dto.RecommendCheckListResponse
import org.project.discipline.domain.checklist.service.RecommendCheckListService
import org.project.discipline.domain.user.dto.CurrentUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/checklist")
@Tag(name = "체크리스트 API", description = "AI 기반 체크리스트 생성 및 관리 API")
class RecommendCheckListController(
    private val checklistService: RecommendCheckListService
) {

    @PostMapping("/generate")
    @Operation(
        summary = "체크리스트 생성",
        description = "목표와 날짜를 기반으로 AI가 체크리스트를 생성합니다. 요청과 응답이 모두 데이터베이스에 추적됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "체크리스트 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    @SecurityRequirement(name = "Bearer Authentication")
    fun generateChecklist(
        @Valid @RequestBody request: RecommendCheckListRequest,
        @Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser?
    ): ResponseEntity<RecommendCheckListResponse> {
        val response = checklistService.generateChecklist(
            request = request,
            currentUser = currentUser
        )
        return ResponseEntity.ok(response)
    }
} 