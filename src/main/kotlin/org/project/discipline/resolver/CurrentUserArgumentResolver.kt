package org.project.discipline.resolver

import org.project.discipline.annotation.CurrentUserInfo
import org.project.discipline.domain.user.dto.CurrentUser
import org.project.discipline.domain.user.service.UserContextService
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException

@Component
class CurrentUserArgumentResolver(
    private val userContextService: UserContextService
) : HandlerMethodArgumentResolver {

    private val logger = LoggerFactory.getLogger(CurrentUserArgumentResolver::class.java)

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(CurrentUserInfo::class.java)
        val isCurrentUserType = parameter.parameterType == CurrentUser::class.java

        logger.debug("CurrentUserArgumentResolver: supportsParameter - hasAnnotation: $hasAnnotation, isCurrentUserType: $isCurrentUserType")

        return hasAnnotation && isCurrentUserType
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        logger.debug("CurrentUserArgumentResolver: resolveArgument called")

        val currentUser = userContextService.getCurrentUser()
        logger.debug("CurrentUserArgumentResolver: CurrentUser resolved: {}", currentUser)

        if (currentUser == null) {
            logger.warn("CurrentUserArgumentResolver: CurrentUser is null")
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")
        }

        return currentUser
    }
}