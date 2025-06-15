package org.project.discipline.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.project.discipline.domain.checklist.dto.RecommendCheckListRequest
import org.project.discipline.domain.checklist.dto.RecommendCheckListResponse
import org.project.discipline.domain.checklist.service.RecommendCheckListService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/test/checklist")
@Tag(name = "체크리스트 테스트 API", description = "인증 없이 테스트할 수 있는 체크리스트 API")
class TestRecommendCheckListController(
    private val checklistService: RecommendCheckListService
) {

    @PostMapping("/generate")
    @Operation(
        summary = "체크리스트 생성 (테스트)",
        description = "인증 없이 체크리스트를 생성합니다. 테스트 목적으로만 사용하세요."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "체크리스트 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun generateChecklist(
        @Valid @RequestBody request: RecommendCheckListRequest
    ): ResponseEntity<RecommendCheckListResponse> {
        val response = checklistService.generateChecklist(
            request = request,
            currentUser = null // 테스트용이므로 null
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/generate/sample")
    @Operation(
        summary = "샘플 체크리스트 생성 (테스트)",
        description = "미리 정의된 샘플 목표로 체크리스트를 생성합니다. 인증이 필요하지 않습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "샘플 체크리스트 생성 성공"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun generateSampleChecklist(): ResponseEntity<RecommendCheckListResponse> {
        val sampleRequest = RecommendCheckListRequest(
            date = LocalDate.now(),
            goal = "오늘 하루 생산적으로 보내기"
        )
        
        val response = checklistService.generateChecklist(
            request = sampleRequest,
            currentUser = null // 테스트용이므로 null
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/generate/quick")
    @Operation(
        summary = "빠른 체크리스트 생성 (테스트)",
        description = "목표만 입력하여 오늘 날짜로 체크리스트를 생성합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "체크리스트 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun generateQuickChecklist(
        @RequestParam goal: String
    ): ResponseEntity<RecommendCheckListResponse> {
        val request = RecommendCheckListRequest(
            date = LocalDate.now(),
            goal = goal
        )
        
        val response = checklistService.generateChecklist(
            request = request,
            currentUser = null // 테스트용이므로 null
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/examples")
    @Operation(
        summary = "예제 목표 목록",
        description = "테스트에 사용할 수 있는 예제 목표들을 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "예제 목록 조회 성공")
        ]
    )
    fun getExamples(): ResponseEntity<List<String>> {
        val examples = listOf(
            "Spring Boot 프로젝트 완성하기",
            "영어 회화 실력 향상시키기",
            "건강한 생활습관 만들기",
            "새로운 기술 스택 학습하기",
            "팀 프로젝트 성공적으로 마무리하기",
            "독서 습관 기르기",
            "운동 루틴 정착시키기",
            "코딩 테스트 준비하기",
            "포트폴리오 업데이트하기",
            "네트워킹 활동 늘리기"
        )
        return ResponseEntity.ok(examples)
    }
} 