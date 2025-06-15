package org.project.discipline.domain.checklist.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.project.discipline.domain.checklist.dto.ChecklistItem
import org.project.discipline.domain.checklist.dto.ChecklistRequest
import org.project.discipline.domain.checklist.dto.ChecklistResponse
import org.project.discipline.domain.checklist.dto.Priority
import org.project.discipline.domain.checklist.entity.ChecklistEntity
import org.project.discipline.domain.checklist.repository.ChecklistRepository
import org.project.discipline.domain.user.dto.CurrentUser
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ChecklistService(
    private val checklistRepository: ChecklistRepository,
    private val objectMapper: ObjectMapper,
    private val chatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(ChecklistService::class.java)

    /**
     * 체크리스트 생성 (통합 처리)
     */
    fun generateChecklist(
        request: ChecklistRequest,
        currentUser: CurrentUser?
    ): ChecklistResponse {
        // date가 null이면 현재 날짜 사용
        val targetDate = request.date ?: LocalDate.now()
        
        // 1. 엔티티 생성 및 저장
        val entity = ChecklistEntity(
            userId = currentUser?.id?.toString(),
            targetDate = targetDate,
            goal = request.goal
        )
        val savedEntity = checklistRepository.save(entity)
        logger.info("Checklist created: id=${savedEntity.id}, userId=${currentUser?.id}, goal=${request.goal}")

        // 2. 처리 시작
        savedEntity.start()
        checklistRepository.save(savedEntity)

        var response: ChecklistResponse
        try {
            // 3. AI 생성
            val prompt = createPrompt(request, targetDate)
            logger.info("Generated prompt for user ${currentUser?.id}: $prompt")

            val aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content() ?: throw IllegalStateException("AI response is null")

            logger.info("AI Response received: ${aiResponse.length} characters")

            // 4. 응답 파싱
            val items = parseAiResponse(aiResponse)
            response = ChecklistResponse(
                date = targetDate,
                goal = request.goal,
                items = items,
                estimatedTotalTime = calculateTotalTime(items)
            )

            // 5. 성공 처리
            val checklistJson = objectMapper.writeValueAsString(items)
            savedEntity.complete(checklistJson)

        } catch (e: Exception) {
            logger.error("Error generating checklist for user ${currentUser?.id}", e)
            
            // 6. 실패 처리
            savedEntity.fail(e.message ?: "Unknown error")
            
            // 7. 폴백 응답 생성
            response = createFallbackChecklist(request, targetDate)
        }

        // 8. 최종 저장
        checklistRepository.save(savedEntity)
        logger.info("Checklist processing completed: id=${savedEntity.id}, status=${savedEntity.status}")

        return response
    }

    /**
     * 사용자별 체크리스트 조회
     */
    fun getUserChecklists(userId: String): List<ChecklistResponse> {
        val entities = checklistRepository.findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.Pageable.unpaged())
        
        return entities.content.mapNotNull { entity ->
            if (entity.isCompleted() && entity.checklistJson != null) {
                try {
                    val items: List<ChecklistItem> = objectMapper.readValue(entity.checklistJson!!)
                    ChecklistResponse(
                        date = entity.targetDate,
                        goal = entity.goal,
                        items = items,
                        estimatedTotalTime = calculateTotalTime(items)
                    )
                } catch (e: Exception) {
                    logger.error("Failed to parse checklist JSON for entity ${entity.id}", e)
                    null
                }
            } else null
        }
    }

    /**
     * AI 프롬프트 생성
     */
    private fun createPrompt(request: ChecklistRequest, targetDate: LocalDate): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        val contextInfo = if (!request.context.isNullOrBlank()) {
            "- 추가 정보: ${request.context}"
        } else ""
        
        return """
            당신은 목표 달성을 위한 체크리스트 생성 전문가입니다.
            주어진 정보:
            - 날짜: ${targetDate.format(dateFormatter)}
            - 목표: ${request.goal}
            $contextInfo
            
            다음 JSON 형식으로 하루 체크리스트를 생성해주세요:
            [
              {
                "task": "구체적인 작업 내용",
                "description": "작업에 대한 상세 설명 (선택사항)",
                "priority": "HIGH|MEDIUM|LOW",
                "estimatedTime": "예상 소요 시간 (예: 30분, 1시간)"
              }
            ]
            
            규칙:
            1. 3-7개의 실행 가능한 작업으로 구성
            2. 우선순위를 명확히 설정 (HIGH: 필수, MEDIUM: 중요, LOW: 선택)
            3. 각 작업은 구체적이고 측정 가능해야 함
            4. 하루 안에 완료 가능한 현실적인 작업들로 구성
            5. 반드시 유효한 JSON 배열 형식으로만 응답
            6. JSON 외의 다른 텍스트는 포함하지 말 것
            
            목표에 맞는 체크리스트를 JSON 형식으로만 응답해주세요:
        """.trimIndent()
    }

    /**
     * AI 응답 파싱
     */
    private fun parseAiResponse(aiResponse: String): List<ChecklistItem> {
        return try {
            val jsonStart = aiResponse.indexOf('[')
            val jsonEnd = aiResponse.lastIndexOf(']') + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                throw IllegalArgumentException("No valid JSON array found in response")
            }
            
            val jsonString = aiResponse.substring(jsonStart, jsonEnd)
            logger.info("Extracted JSON: $jsonString")
            
            val rawItems: List<Map<String, Any>> = objectMapper.readValue(jsonString)
            
            rawItems.map { item ->
                ChecklistItem(
                    task = item["task"]?.toString() ?: "작업 내용 없음",
                    description = item["description"]?.toString(),
                    priority = when (item["priority"]?.toString()?.uppercase()) {
                        "HIGH" -> Priority.HIGH
                        "LOW" -> Priority.LOW
                        else -> Priority.MEDIUM
                    },
                    estimatedTime = item["estimatedTime"]?.toString()
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to parse AI response", e)
            throw IllegalArgumentException("Invalid AI response format", e)
        }
    }

    /**
     * 총 예상 시간 계산
     */
    private fun calculateTotalTime(items: List<ChecklistItem>): String {
        val times = items.mapNotNull { it.estimatedTime }.joinToString(", ")
        return if (times.isNotEmpty()) "총 예상 시간: $times" else "시간 정보 없음"
    }

    /**
     * 폴백 체크리스트 생성
     */
    private fun createFallbackChecklist(request: ChecklistRequest, targetDate: LocalDate): ChecklistResponse {
        val fallbackItems = listOf(
            ChecklistItem(
                task = "목표 세부 계획 세우기",
                description = "${request.goal}을(를) 달성하기 위한 구체적인 계획을 세워보세요",
                priority = Priority.HIGH,
                estimatedTime = "30분"
            ),
            ChecklistItem(
                task = "필요한 자료 준비하기",
                description = "목표 달성에 필요한 자료나 도구를 준비하세요",
                priority = Priority.MEDIUM,
                estimatedTime = "20분"
            ),
            ChecklistItem(
                task = "첫 번째 단계 실행하기",
                description = "계획한 첫 번째 단계를 실행해보세요",
                priority = Priority.HIGH,
                estimatedTime = "1시간"
            )
        )

        return ChecklistResponse(
            date = targetDate,
            goal = request.goal,
            items = fallbackItems,
            estimatedTotalTime = calculateTotalTime(fallbackItems)
        )
    }
} 