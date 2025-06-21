package org.project.discipline.domain.checklist.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.project.discipline.domain.checklist.dto.RecommendCheckListItem
import org.project.discipline.domain.checklist.dto.RecommendCheckListRequest
import org.project.discipline.domain.checklist.dto.RecommendCheckListResponse
import org.project.discipline.domain.checklist.entity.RecommendCheckListEntity
import org.project.discipline.domain.checklist.repository.RecommendCheckListRepository
import org.project.discipline.domain.user.dto.CurrentUser
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional
class RecommendCheckListService(
    private val checklistRepository: RecommendCheckListRepository,
    private val objectMapper: ObjectMapper,
    private val chatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(RecommendCheckListService::class.java)

    /**
     * 체크리스트 생성 (통합 처리)
     */
    fun generateChecklist(
        request: RecommendCheckListRequest,
        currentUser: CurrentUser?
    ): RecommendCheckListResponse {
        // 항상 현재 날짜 사용
        val targetDate = LocalDate.now()

        // 1. 엔티티 생성 및 저장
        val entity = RecommendCheckListEntity(
            userId = currentUser?.id?.toString(),
            targetDate = targetDate,
            goal = request.goal
        )
        val savedEntity = checklistRepository.save(entity)
        logger.info("Checklist created: id=${savedEntity.id}, userId=${currentUser?.id}, goal=${request.goal}")

        // 2. 처리 시작
        savedEntity.start()
        checklistRepository.save(savedEntity)

        try {
            // 3. AI 생성 (Prompt 객체 사용)
            val items = generateChecklistItemsWithAI(request, targetDate)
            val response = RecommendCheckListResponse(
                date = targetDate,
                goal = request.goal,
                items = items,
                estimatedTotalTime = calculateTotalTime(items)
            )

            // 4. 성공 처리
            val checklistJson = objectMapper.writeValueAsString(items)
            savedEntity.complete(checklistJson)
            checklistRepository.save(savedEntity)

            logger.info("Checklist processing completed successfully: id=${savedEntity.id}")
            return response

        } catch (e: Exception) {
            logger.error("Error generating checklist for user ${currentUser?.id}", e)

            // 5. 실패 처리
            savedEntity.fail(e.message ?: "Unknown error")
            checklistRepository.save(savedEntity)

            // 6. 예외를 다시 던져서 500 오류 반환
            throw e
        }
    }

    /**
     * Prompt 객체를 사용한 AI 체크리스트 생성
     */
    private fun generateChecklistItemsWithAI(
        request: RecommendCheckListRequest,
        targetDate: LocalDate
    ): List<RecommendCheckListItem> {
        logger.info("Using Prompt object with BeanOutputConverter for structured output generation")

        // 1. Prompt 객체 생성
        val prompt = createPromptObject(request, targetDate)
        logger.info("Created prompt with ${prompt.instructions.size} instructions")

        // 2. ChatClient에 Prompt 객체 전달하여 구조화된 응답 생성
        val items: List<RecommendCheckListItem>? = chatClient
            .prompt(prompt)
            .call()
            .entity(object : ParameterizedTypeReference<List<RecommendCheckListItem>>() {})

        // 3. null 체크 및 빈 리스트 체크 - 실패 시 예외 던지기
        return when {
            items.isNullOrEmpty() -> {
                logger.error("AI returned null or empty response for goal: ${request.goal}")
                throw IllegalStateException("AI가 체크리스트를 생성하지 못했습니다. 잠시 후 다시 시도해주세요.")
            }

            else -> {
                logger.info("AI successfully generated ${items.size} checklist items")
                items
            }
        }
    }

    /**
     * Prompt 객체 생성
     */
    private fun createPromptObject(request: RecommendCheckListRequest, targetDate: LocalDate): Prompt {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        val converter = BeanOutputConverter(object : ParameterizedTypeReference<List<RecommendCheckListItem>>() {})

        // PromptTemplate 사용하여 더 구조화된 프롬프트 생성
        val templateText = """
            당신은 목표 달성을 위한 체크리스트 생성 전문가입니다.
            주어진 정보:
            - 날짜: {date}
            - 목표: {goal}
            
            규칙:
            1. 3-7개의 실행 가능한 작업으로 구성
            2. 우선순위를 명확히 설정 (HIGH: 필수, MEDIUM: 중요, LOW: 선택)
            3. 각 작업은 구체적이고 측정 가능해야 함
            4. 하루 안에 완료 가능한 현실적인 작업들로 구성
            5. description은 각 작업에 대한 구체적인 설명을 포함
            6. estimatedDuration는 각 작업에 대한 구체적인 소요시간을 포함. epoch milli를 기준으로 계산(1000 -> 1초, 6000 -> 6초)
            
            목표에 맞는 하루 체크리스트를 생성해주세요.
        """.trimIndent()

        logger.info("templateText: $templateText")

        val promptTemplate = PromptTemplate(templateText)

        // 템플릿 변수 설정
        val templateVars = mapOf(
            "date" to targetDate.format(dateFormatter),
            "goal" to request.goal,
            "outputFormat" to converter.format
        )

        logger.debug("Creating prompt with variables: {}", templateVars)

        return promptTemplate.create(templateVars)
    }

    /**
     * 총 예상 시간 계산
     */
    private fun calculateTotalTime(items: List<RecommendCheckListItem>): String {
        val times = items.map { it.estimatedDuration }.joinToString(", ")
        return if (times.isNotEmpty()) "총 예상 시간: $times" else "시간 정보 없음"
    }
} 