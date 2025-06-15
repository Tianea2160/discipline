package org.project.discipline.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag

import jakarta.validation.Valid
import org.project.discipline.annotation.CurrentUserInfo
import org.project.discipline.domain.checklist.dto.ChecklistRequest
import org.project.discipline.domain.checklist.dto.ChecklistResponse
import org.project.discipline.domain.user.dto.CurrentUser
import org.project.discipline.domain.checklist.service.ChecklistService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/checklist")
@Tag(name = "체크리스트 API", description = "AI 기반 체크리스트 생성 및 관리 API")
class ChecklistController(
    private val checklistService: ChecklistService
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
        @Valid @RequestBody request: ChecklistRequest,
        @Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser?
    ): ResponseEntity<ChecklistResponse> {
        val response = checklistService.generateChecklist(
            request = request,
            currentUser = currentUser
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/generate/sample")
    @Operation(
        summary = "샘플 체크리스트 생성",
        description = "미리 정의된 샘플 목표로 체크리스트를 생성합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "샘플 체크리스트 생성 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    @SecurityRequirement(name = "Bearer Authentication")
    fun generateSampleChecklist(
        @Parameter(hidden = true) @CurrentUserInfo currentUser: CurrentUser?
    ): ResponseEntity<ChecklistResponse> {
        val sampleRequest = ChecklistRequest(
            date = LocalDate.now(),
            goal = "오늘 하루 생산적으로 보내기"
        )

        val response = checklistService.generateChecklist(
            request = sampleRequest,
            currentUser = currentUser
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/templates")
    @Operation(
        summary = "체크리스트 템플릿 목록",
        description = "자주 사용되는 목표 템플릿 목록을 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "템플릿 목록 조회 성공")
        ]
    )
    fun getTemplates(): ResponseEntity<Map<String, List<String>>> {
        val templates = mapOf(
            "학습" to listOf(
                "영어 공부하기",
                "새로운 프로그래밍 언어 배우기",
                "온라인 강의 수강하기",
                "독서하기"
            ),
            "건강" to listOf(
                "운동하기",
                "건강한 식단 유지하기",
                "충분한 수면 취하기",
                "스트레칭하기"
            ),
            "업무" to listOf(
                "프로젝트 완료하기",
                "회의 준비하기",
                "업무 효율성 높이기",
                "팀 협업 개선하기"
            ),
            "개발" to listOf(
                "새로운 기능 개발하기",
                "코드 리팩토링하기",
                "테스트 코드 작성하기",
                "문서화 작업하기"
            ),
            "취미" to listOf(
                "새로운 취미 시작하기",
                "창작 활동하기",
                "여행 계획 세우기",
                "친구들과 시간 보내기"
            )
        )
        return ResponseEntity.ok(templates)
    }
} 