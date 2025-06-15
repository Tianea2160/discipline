package org.project.discipline.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DiscordNotificationService(
    private val webClient: WebClient = WebClient.builder().build()
) {
    
    private val logger = LoggerFactory.getLogger(DiscordNotificationService::class.java)
    
    @Value("\${discord.webhook.url:}")
    private lateinit var webhookUrl: String
    
    @Value("\${discord.webhook.enabled:false}")
    private var enabled: Boolean = false
    
    fun sendErrorNotification(
        error: Throwable,
        requestUri: String? = null,
        userAgent: String? = null,
        userId: String? = null
    ) {
        if (!enabled || webhookUrl.isBlank()) {
            logger.debug("Discord notification disabled or webhook URL not configured")
            return
        }
        
        try {
            val embed = createErrorEmbed(error, requestUri, userAgent, userId)
            val payload = DiscordWebhookPayload(
                content = "🚨 **서버 오류 발생**",
                embeds = listOf(embed)
            )
            
            webClient.post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String::class.java)
                .doOnSuccess { 
                    logger.info("Discord notification sent successfully") 
                }
                .doOnError { ex -> 
                    logger.error("Failed to send Discord notification", ex) 
                }
                .onErrorResume { Mono.empty() }
                .subscribe()
                
        } catch (ex: Exception) {
            logger.error("Error creating Discord notification", ex)
        }
    }
    
    private fun createErrorEmbed(
        error: Throwable,
        requestUri: String?,
        userAgent: String?,
        userId: String?
    ): DiscordEmbed {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        val fields = mutableListOf<DiscordEmbedField>().apply {
            add(DiscordEmbedField("오류 타입", error.javaClass.simpleName, true))
            add(DiscordEmbedField("발생 시간", timestamp, true))
            
            requestUri?.let { 
                add(DiscordEmbedField("요청 URI", it, false))
            }
            
            userId?.let {
                add(DiscordEmbedField("사용자 ID", it, true))
            }
            
            userAgent?.let {
                add(DiscordEmbedField("User Agent", it.take(100), false))
            }
            
            error.message?.let {
                add(DiscordEmbedField("오류 메시지", it.take(500), false))
            }
            
            // 스택 트레이스의 첫 몇 줄만 포함
            val stackTrace = error.stackTrace.take(5)
                .joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
            if (stackTrace.isNotEmpty()) {
                add(DiscordEmbedField("스택 트레이스", "```\n$stackTrace\n```", false))
            }
        }
        
        return DiscordEmbed(
            title = "🔥 서버 500 오류",
            description = "애플리케이션에서 예상치 못한 오류가 발생했습니다.",
            color = 15158332, // 빨간색
            fields = fields,
            timestamp = LocalDateTime.now().toString()
        )
    }
}

data class DiscordWebhookPayload(
    val content: String,
    val embeds: List<DiscordEmbed>
)

data class DiscordEmbed(
    val title: String,
    val description: String,
    val color: Int,
    val fields: List<DiscordEmbedField>,
    val timestamp: String
)

data class DiscordEmbedField(
    val name: String,
    val value: String,
    val inline: Boolean = false
) 