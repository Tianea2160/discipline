package org.project.discipline.exception

import jakarta.servlet.http.HttpServletRequest
import org.project.discipline.domain.user.service.UserContextService
import org.project.discipline.notification.DiscordNotificationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler(
    private val discordNotificationService: DiscordNotificationService,
    private val userContextService: UserContextService
) {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Resource not found: {}", request.requestURI)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = "요청한 리소스를 찾을 수 없습니다.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid argument: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "잘못된 요청입니다.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(RecommendCheckListGenerationException::class)
    fun handleRecommendCheckListGenerationException(
        ex: RecommendCheckListGenerationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Recommend check list generation failed: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Recommend Check List Generation Error",
            message = "추천 체크리스트 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(InvalidRecommendCheckListFormatException::class)
    fun handleInvalidRecommendCheckListFormatException(
        ex: InvalidRecommendCheckListFormatException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid recommend check list format: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Invalid Format",
            message = "추천 체크리스트 형식이 올바르지 않습니다.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Runtime exception occurred", ex)

        // 500 이상의 오류에 대해서만 Discord 알림 전송
        sendDiscordNotificationForServerError(ex, request)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "서버에서 오류가 발생했습니다.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception occurred", ex)

        // 500 이상의 오류에 대해서만 Discord 알림 전송
        sendDiscordNotificationForServerError(ex, request)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "서버에서 예상치 못한 오류가 발생했습니다.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * 500 이상의 서버 오류에 대해서만 Discord 알림을 전송하는 메서드
     */
    private fun sendDiscordNotificationForServerError(
        ex: Exception,
        request: HttpServletRequest
    ) {
        try {
            // 현재 사용자 정보 가져오기 (있다면)
            val currentUser = try {
                userContextService.getCurrentUser()
            } catch (e: Exception) {
                null
            }
            
            // Discord 알림 전송 (500+ 오류만)
            discordNotificationService.sendErrorNotification(
                error = ex,
                requestUri = request.requestURI,
                userAgent = request.getHeader("User-Agent"),
                userId = currentUser?.id.toString()
            )
        } catch (notificationEx: Exception) {
            logger.error("Failed to send Discord notification", notificationEx)
        }
    }
}

data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String
) 