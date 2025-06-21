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
            당신은 개인 맞춤형 목표 달성 체크리스트 생성 전문가입니다.
            사용자의 목표를 분석하고, 실행 가능하고 동기부여가 되는 하루 체크리스트를 생성해주세요.
            
            **주어진 정보:**
            - 날짜: {date}
            - 목표: {goal}
            
            **체크리스트 생성 원칙:**
            
            1. **작업 구성 (3-7개)**
               - 목표를 달성 가능한 단계로 분해
               - 각 작업은 SMART 기준 적용 (구체적, 측정가능, 달성가능, 관련성, 시간제한)
               - 작업 간 논리적 순서와 의존성 고려
               - 에너지 레벨과 중요도를 고려하여 작업 순서 배치
            
            2. **우선순위 설정**
               - HIGH: 목표 달성에 필수적인 핵심 작업 (1-2개)
               - MEDIUM: 목표 진전에 중요한 작업 (2-3개)  
               - LOW: 추가적 가치를 제공하는 선택적 작업 (1-2개)
            
            3. **시간 관리**
               - 예상 소요시간(estimatedDuration)은 밀리초 단위 (1800000=30분, 3600000=1시간)
               - 전체 작업 시간이 6-8시간을 초과하지 않도록 조정
               - 휴식 시간과 버퍼 타임을 고려한 현실적 계획
               - 각 작업은 최대 4시간을 넘지 않도록 분할
            
            4. **실행 가능성**
               - 하루 안에 완료 가능한 현실적 범위
               - 필요한 리소스와 도구의 접근성 확인
               - 예상되는 장애물과 대안책 제시
               - 외부 의존성을 최소화하고 자기 통제 가능한 작업 우선
            
            5. **동기부여 요소**
               - 각 작업 완료 시의 성취감을 높이는 구체적 설명
               - 목표와의 직접적 연관성을 명확히 제시
               - 진행 상황을 확인할 수 있는 구체적이고 측정 가능한 지표
               - 작업 완료 후 느낄 수 있는 긍정적 변화 명시
            
            **추가 고려사항:**
            - 목표가 모호한 경우, 더 구체적인 하위 목표로 세분화
            - 예상치 못한 상황을 위한 유연성 확보 (20% 버퍼 시간)
            - 작업명은 동사로 시작하는 명확한 액션으로 구성
            - 각 작업에는 완료 확인이 가능한 구체적 기준 포함
            - 개인의 집중력과 에너지 패턴을 고려한 작업 배치
            
            목표: "{goal}"를 달성하기 위한 최적화된 하루 체크리스트를 생성해주세요.
            각 작업은 구체적이고 실행 가능하며, 완료 여부를 명확히 판단할 수 있어야 합니다.
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