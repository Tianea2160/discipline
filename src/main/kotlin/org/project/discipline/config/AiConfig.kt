package org.project.discipline.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {
    
    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient {
        return builder
            .defaultSystem("당신은 목표 달성을 위한 체크리스트 생성 전문가입니다. 항상 한국어로 응답하고, 실용적이고 구체적인 작업들을 제안해주세요.")
            .build()
    }
} 