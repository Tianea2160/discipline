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
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
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
     * 🔍 커스텀 로깅 Advisor
     */
    private val loggingAdvisor = object : CallAroundAdvisor {
        override fun getName(): String = "LoggingAdvisor"

        override fun getOrder(): Int = 0  // 첫 번째로 실행

        override fun aroundCall(advisedRequest: AdvisedRequest, chain: CallAroundAdvisorChain): AdvisedResponse {
            val startTime = System.currentTimeMillis()

            // 🚀 요청 로깅
            logger.info("🚀 AI REQUEST START")
            logger.info("📝 Prompt Messages: {}", advisedRequest.chatModel().toString())
            advisedRequest.userText().let { userText ->
                logger.info("👤 User Message: {}", userText)
            }

            try {
                // 체인의 다음 advisor 또는 실제 AI 호출 실행
                val response = chain.nextAroundCall(advisedRequest)

                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                // ✅ 응답 로깅 (성공)
                logger.info("✅ AI RESPONSE SUCCESS - Duration: {}ms", duration)
                logger.info(
                    "🤖 Response Content: {}",
                    response.response()?.result?.output?.content?.take(1000) ?: "No content"
                )

                return response

            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                // ❌ 응답 로깅 (실패)
                logger.error("❌ AI RESPONSE ERROR - Duration: {}ms, Error: {}", duration, e.message, e)
                throw e
            }
        }
    }

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
     * Advisor를 사용한 AI 체크리스트 생성
     */
    private fun generateChecklistItemsWithAI(
        request: RecommendCheckListRequest,
        targetDate: LocalDate
    ): List<RecommendCheckListItem> {
        logger.info("🎯 Starting AI checklist generation with logging advisor")

        // 1. Prompt 객체 생성
        val prompt = createPromptObject(request, targetDate)

        // 2. 로깅 Advisor와 함께 ChatClient 호출
        val items = chatClient
            .prompt(prompt)
            .advisors(loggingAdvisor)  // 🔍 로깅 Advisor 추가
            .call()
            .entity(object : ParameterizedTypeReference<List<RecommendCheckListItem>>() {})

        // 3. 결과 처리
        return when {
            items.isNullOrEmpty() -> {
                logger.error("❌ AI returned null or empty response for goal: ${request.goal}")
                throw IllegalStateException("AI가 체크리스트를 생성하지 못했습니다. 잠시 후 다시 시도해주세요.")
            }

            else -> {
                logger.info("🎉 Successfully generated ${items.size} checklist items")
                items.forEachIndexed { index, item ->
                    logger.debug("📋 Item[{}]: {} ({})", index, item.task, item.priority)
                }
                items
            }
        }
    }

    /**
     * Prompt 객체 생성 (비즈니스 로직만 포함)
     */
    private fun createPromptObject(request: RecommendCheckListRequest, targetDate: LocalDate): Prompt {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")

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
            6. estimatedDuration은 밀리초 단위의 예상 소요시간 (예: 1800000 = 30분, 3600000 = 1시간)
            
            목표에 맞는 하루 체크리스트를 생성해주세요.
        """.trimIndent()

        logger.debug("Generated template for goal: {}", request.goal)

        val promptTemplate = PromptTemplate(templateText)

        // 템플릿 변수 설정
        val templateVars = mapOf(
            "date" to targetDate.format(dateFormatter),
            "goal" to request.goal,
        )

        logger.debug("Creating prompt with variables: {}", templateVars.keys)

        return promptTemplate.create(templateVars)
    }

    /**
     * 총 예상 시간 계산 (밀리초를 사람이 읽기 쉬운 형태로 변환)
     */
    private fun calculateTotalTime(items: List<RecommendCheckListItem>): String {
        val totalMillis = items.sumOf { it.estimatedDuration }

        if (totalMillis <= 0) return "시간 정보 없음"

        val hours = totalMillis / 3600000
        val minutes = (totalMillis % 3600000) / 60000

        return when {
            hours > 0 && minutes > 0 -> "총 예상 시간: ${hours}시간 ${minutes}분"
            hours > 0 -> "총 예상 시간: ${hours}시간"
            minutes > 0 -> "총 예상 시간: ${minutes}분"
            else -> "총 예상 시간: 1분 미만"
        }
    }
} 