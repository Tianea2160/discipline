package org.project.discipline.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.project.discipline.annotation.CurrentUserInfo
import org.project.discipline.annotation.RequireAuth
import org.project.discipline.domain.user.dto.CurrentUser
import org.project.discipline.domain.user.service.UserContextService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Aspect
// @Component  // ArgumentResolver 사용으로 인해 임시 비활성화
class UserContextAspect(
    private val userContextService: UserContextService
) {
    private val logger = LoggerFactory.getLogger(UserContextAspect::class.java)

    @Around("@annotation(requireAuth)")
    fun checkAuthentication(joinPoint: ProceedingJoinPoint, requireAuth: RequireAuth): Any? {
        val currentUser = userContextService.getCurrentUser()

        if (currentUser == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")
        }

        // 역할 체크
        if (requireAuth.roles.isNotEmpty()) {
            val hasRequiredRole = requireAuth.roles.any { role -> currentUser.hasRole(role) }
            if (!hasRequiredRole) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 부족합니다.")
            }
        }

        return proceedWithUserInjection(joinPoint, currentUser)
    }

    // @CurrentUserInfo 어노테이션이 붙은 파라미터가 있는 컨트롤러와 서비스 메서드를 대상으로 함
    @Around("execution(* org.project.discipline.controller..*(..)) || execution(* org.project.discipline.service..*(..))")
    fun injectCurrentUserInAnyMethod(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val parameters = method.parameters

        val hasCurrentUserInfoParam = parameters.any {
            it.isAnnotationPresent(CurrentUserInfo::class.java) &&
                    it.type == CurrentUser::class.java
        }

        if (!hasCurrentUserInfoParam) return joinPoint.proceed()

        logger.debug("UserContextAspect: Method with @CurrentUserInfo parameter called: ${method.name}")
        
        val currentUser = userContextService.getCurrentUser()
        logger.debug("UserContextAspect: CurrentUser returned from userContextService: {}", currentUser)

        if (currentUser == null) {
            logger.warn("UserContextAspect: CurrentUser is null")
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")
        }

        logger.debug("UserContextAspect: Attempting to inject currentUser - email: '${currentUser.email}', name: '${currentUser.name}'")
        return proceedWithUserInjection(joinPoint, currentUser)
    }

    private fun proceedWithUserInjection(joinPoint: ProceedingJoinPoint, currentUser: CurrentUser): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val parameters = method.parameters
        val args = joinPoint.args.toMutableList()

        logger.debug("UserContextAspect: Starting proceedWithUserInjection - currentUser: {}", currentUser)
        logger.debug("UserContextAspect: Original args size: ${args.size}, parameters size: ${parameters.size}")

        parameters.forEachIndexed { index, parameter ->
            logger.debug(
                "UserContextAspect: Parameter {} - type: {}, @CurrentUserInfo: {}",
                index,
                parameter.type,
                parameter.isAnnotationPresent(CurrentUserInfo::class.java)
            )

            if (parameter.isAnnotationPresent(CurrentUserInfo::class.java) &&
                parameter.type == CurrentUser::class.java
            ) {
                while (args.size <= index) {
                    args.add(null)
                }

                logger.debug("UserContextAspect: Injecting currentUser to parameter $index: $currentUser")
                args[index] = currentUser
            }
        }

        logger.debug("UserContextAspect: Final args: {}", args.map { it?.javaClass?.simpleName ?: "null" })

        return try {
            val result = joinPoint.proceed(args.toTypedArray())
            logger.debug("UserContextAspect: Method execution successful")
            result
        } catch (e: Exception) {
            logger.error("UserContextAspect: Error during method execution", e)
            throw e
        }
    }
} 